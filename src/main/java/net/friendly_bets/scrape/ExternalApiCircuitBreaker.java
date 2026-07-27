package net.friendly_bets.scrape;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * After consecutive trip-worthy failures, disables the layer in MongoDB until an admin re-enables it.
 */
@Service
@RequiredArgsConstructor
public class ExternalApiCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiCircuitBreaker.class);

    public static final String CODE_LAYER_CIRCUIT_OPEN = "layerCircuitOpen";

    private final ExternalDataLayerConfigService layerConfigService;
    private final ErrorLogService errorLogService;

    @Value("${external-data.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    private final Map<ExternalDataLayer, AtomicInteger> consecutiveFailures = new ConcurrentHashMap<>();

    public void recordSuccess(ExternalDataLayer layer) {
        if (layer == null) {
            return;
        }
        consecutiveFailures.computeIfAbsent(layer, k -> new AtomicInteger()).set(0);
    }

    public void recordFailure(
            ExternalDataLayer layer,
            String providerId,
            ScrapeFailureKind kind,
            String detail
    ) {
        if (layer == null || !isTripWorthy(kind)) {
            return;
        }
        int threshold = Math.max(1, failureThreshold);
        int count = consecutiveFailures
                .computeIfAbsent(layer, k -> new AtomicInteger())
                .incrementAndGet();
        log.warn("scrape circuit {} provider={} kind={} failures={}/{} detail={}",
                layer, providerId, kind, count, threshold, detail);
        if (count < threshold) {
            return;
        }
        consecutiveFailures.get(layer).set(0);
        String message = "Circuit open after " + count + " consecutive " + kind
                + (detail != null && !detail.isBlank() ? ": " + detail : "");
        boolean disabled = layerConfigService.disableLayer(layer, message);
        errorLogService.record(ErrorLogService.Entry.builder()
                .severity(ErrorLogService.SEVERITY_ERROR)
                .layer(layer.name())
                .provider(providerId)
                .code(CODE_LAYER_CIRCUIT_OPEN)
                .message(message)
                .context(Map.of(
                        "kind", kind.name(),
                        "disabled", String.valueOf(disabled),
                        "threshold", String.valueOf(threshold)
                ))
                .build());
        if (disabled) {
            log.error("Disabled external-data layer {} after circuit trip (provider={})", layer, providerId);
        }
    }

    private static boolean isTripWorthy(ScrapeFailureKind kind) {
        return kind == ScrapeFailureKind.TIMEOUT
                || kind == ScrapeFailureKind.NETWORK_ERROR
                || kind == ScrapeFailureKind.HTTP_BLOCKED
                || kind == ScrapeFailureKind.CHALLENGE;
    }
}
