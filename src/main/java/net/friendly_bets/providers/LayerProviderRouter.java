package net.friendly_bets.providers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.FullMatchNotReadyException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Runs a layer operation on primary provider; retries once with secondary only on HTTP/transport failures.
 */
@Component
@RequiredArgsConstructor
public class LayerProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(LayerProviderRouter.class);

    private final ExternalDataLayerConfigService configService;
    private final LayerProviderRegistry registry;
    private final ErrorLogService errorLogService;

    public <T extends ExternalDataProvider, R> R execute(
            ExternalDataLayer layer,
            Class<T> type,
            Function<T, R> operation
    ) {
        return execute(layer, type, operation, null);
    }

    public <T extends ExternalDataProvider, R> R execute(
            ExternalDataLayer layer,
            Class<T> type,
            Function<T, R> operation,
            String leagueCode
    ) {
        AppSettings.LayerAssignment assignment = configService.assignment(layer);
        String primaryId = assignment.getPrimaryProvider();
        if (primaryId == null || primaryId.isBlank()) {
            errorLogService.recordLayerFailure(
                    layer, null, ErrorLogService.ROLE_PRIMARY,
                    "externalDataLayerPrimaryMissing",
                    "Primary provider is not configured for layer " + layer,
                    leagueCode
            );
            throw new BadRequestException("externalDataLayerPrimaryMissing");
        }
        T primary = registry.findAs(primaryId, type)
                .orElseThrow(() -> {
                    errorLogService.recordLayerFailure(
                            layer, primaryId, ErrorLogService.ROLE_PRIMARY,
                            "externalDataProviderUnavailable",
                            "Primary provider bean unavailable for layer " + layer,
                            leagueCode
                    );
                    return new BadRequestException("externalDataProviderUnavailable");
                });
        if (!primary.supports(layer)) {
            errorLogService.recordLayerFailure(
                    layer, primaryId, ErrorLogService.ROLE_PRIMARY,
                    "externalDataProviderUnavailable",
                    "Primary provider does not support layer " + layer,
                    leagueCode
            );
            throw new BadRequestException("externalDataProviderUnavailable");
        }
        try {
            return operation.apply(primary);
        } catch (FullMatchNotReadyException notReady) {
            // Deferral signal — not a provider outage; do not failover or treat as layer failure.
            throw notReady;
        } catch (RuntimeException primaryError) {
            if (!ExternalApiHttpFailures.isHttpTransportFailure(primaryError)) {
                errorLogService.recordLayerFailure(
                        layer, primaryId, ErrorLogService.ROLE_PRIMARY,
                        resolveErrorCode(primaryError),
                        primaryError.getMessage(),
                        leagueCode
                );
                throw primaryError;
            }
            String secondaryId = assignment.getSecondaryProvider();
            if (secondaryId == null || secondaryId.isBlank() || secondaryId.equals(primaryId)) {
                errorLogService.recordLayerFailure(
                        layer, primaryId, ErrorLogService.ROLE_PRIMARY,
                        resolveErrorCode(primaryError),
                        primaryError.getMessage(),
                        leagueCode
                );
                throw primaryError;
            }
            T secondary = registry.findAs(secondaryId, type).orElse(null);
            if (secondary == null || !secondary.supports(layer)) {
                errorLogService.recordLayerFailure(
                        layer, primaryId, ErrorLogService.ROLE_PRIMARY,
                        resolveErrorCode(primaryError),
                        primaryError.getMessage(),
                        leagueCode
                );
                throw primaryError;
            }
            log.warn("Layer {} primary {} failed ({}), trying secondary {}",
                    layer, primaryId, primaryError.getMessage(), secondaryId);
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(ErrorLogService.SEVERITY_WARN)
                    .layer(layer.name())
                    .provider(primaryId)
                    .providerRole(ErrorLogService.ROLE_PRIMARY)
                    .code(ErrorLogService.CODE_PRIMARY_UNAVAILABLE)
                    .message(primaryError.getMessage())
                    .leagueCode(leagueCode)
                    .build());
            try {
                return operation.apply(secondary);
            } catch (RuntimeException secondaryError) {
                log.warn("Layer {} secondary {} also failed: {}", layer, secondaryId, secondaryError.getMessage());
                errorLogService.recordLayerFailure(
                        layer, secondaryId, ErrorLogService.ROLE_SECONDARY,
                        ErrorLogService.CODE_SECONDARY_UNAVAILABLE,
                        secondaryError.getMessage(),
                        leagueCode
                );
                throw secondaryError;
            }
        }
    }

    private static String resolveErrorCode(RuntimeException error) {
        if (error instanceof BadRequestException && error.getMessage() != null && !error.getMessage().isBlank()) {
            return error.getMessage().trim();
        }
        return ErrorLogService.CODE_LAYER_FAILED;
    }
}
