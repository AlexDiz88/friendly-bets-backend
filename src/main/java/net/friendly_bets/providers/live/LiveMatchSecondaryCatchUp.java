package net.friendly_bets.providers.live;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRegistry;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * When primary LIVE leaves a match non-terminal past a typical match length,
 * poll the configured LIVE secondary for an independent status (no auto-FINISHED).
 */
@Component
@RequiredArgsConstructor
public class LiveMatchSecondaryCatchUp {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchSecondaryCatchUp.class);

    private final ExternalDataLayerConfigService layerConfigService;
    private final LayerProviderRegistry layerProviderRegistry;
    private final ErrorLogService errorLogService;

    public Optional<LiveMatchProvider.LiveSyncResult> catchUpIfNeeded(
            Season season,
            List<MatchSchedule> seasonSchedules,
            Instant now
    ) {
        if (season == null || seasonSchedules == null || seasonSchedules.isEmpty() || now == null) {
            return Optional.empty();
        }
        long overdue = seasonSchedules.stream()
                .filter(s -> LiveMatchSupport.needsSecondaryStatusCatchUp(s, now))
                .count();
        if (overdue <= 0) {
            return Optional.empty();
        }

        AppSettings.LayerAssignment assignment = layerConfigService.assignment(ExternalDataLayer.LIVE);
        String primaryId = assignment != null ? assignment.getPrimaryProvider() : null;
        String secondaryId = assignment != null ? assignment.getSecondaryProvider() : null;
        if (secondaryId == null || secondaryId.isBlank() || secondaryId.equals(primaryId)) {
            log.warn("LIVE secondary catch-up needed for {} match(es) but secondary is not configured", overdue);
            String sampleId = seasonSchedules.stream()
                    .filter(s -> LiveMatchSupport.needsSecondaryStatusCatchUp(s, now))
                    .map(MatchSchedule::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(null);
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(ErrorLogService.SEVERITY_WARN)
                    .layer(ExternalDataLayer.LIVE.name())
                    .provider(primaryId)
                    .code("liveSecondaryCatchUpUnavailable")
                    .message("Матч(и) дольше обычной длительности без терминального статуса ("
                            + overdue + "), но LIVE secondary не назначен")
                    .season(season.getId())
                    .matchScheduleId(sampleId)
                    .context(java.util.Map.of(
                            "overdueCount", String.valueOf(overdue),
                            "catchUpAfterSec", String.valueOf(LiveMatchSupport.SECONDARY_CATCHUP_AFTER_KICKOFF_SECONDS)
                    ))
                    .dedupeByMatch(sampleId != null)
                    .build());
            return Optional.empty();
        }

        Optional<LiveMatchProvider> secondaryOpt =
                layerProviderRegistry.findAs(secondaryId, LiveMatchProvider.class);
        if (secondaryOpt.isEmpty() || !secondaryOpt.get().supports(ExternalDataLayer.LIVE)) {
            errorLogService.recordLayerFailure(
                    ExternalDataLayer.LIVE,
                    secondaryId,
                    ErrorLogService.ROLE_SECONDARY,
                    ErrorLogService.CODE_SECONDARY_UNAVAILABLE,
                    "LIVE secondary bean unavailable for overdue catch-up",
                    "ALL"
            );
            return Optional.empty();
        }

        LiveMatchProvider secondary = secondaryOpt.get();
        log.info("LIVE secondary catch-up via {} for {} overdue non-terminal match(es)", secondaryId, overdue);
        ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ORCHESTRATOR);
        try {
            return Optional.of(secondary.syncLive(season));
        } finally {
            ExternalApiMonitoringService.clearTriggerOverride();
        }
    }
}
