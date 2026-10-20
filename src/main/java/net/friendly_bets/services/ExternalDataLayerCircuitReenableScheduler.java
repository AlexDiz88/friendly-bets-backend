package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * After circuit trip ({@code enabled=false} + {@code circuit_disabled_at}), re-enables the layer
 * once {@code external-data.circuit-breaker.reenable-after-hours} elapses so autosync can retry.
 * Admin disables without a stamp are not auto-re-enabled.
 */
@Component
@RequiredArgsConstructor
public class ExternalDataLayerCircuitReenableScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalDataLayerCircuitReenableScheduler.class);

    private final ExternalDataLayerAutoDisableService layerAutoDisableService;
    private final ErrorLogService errorLogService;

    @Value("${external-data.circuit-breaker.reenable-after-hours:6}")
    private long reenableAfterHours;

    @Scheduled(fixedDelayString = "${external-data.circuit-breaker.reenable-check-ms:60000}")
    public void reenableExpiredCircuits() {
        Duration cooldown = Duration.ofHours(Math.max(1L, reenableAfterHours));
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            try {
                if (!layerAutoDisableService.reenableIfCircuitCooldownElapsed(layer, cooldown)) {
                    continue;
                }
                log.warn("Auto-re-enabled external-data layer {} after circuit cooldown ({}h)",
                        layer, cooldown.toHours());
                errorLogService.record(ErrorLogService.Entry.builder()
                        .severity(ErrorLogService.SEVERITY_WARN)
                        .layer(layer.name())
                        .code(ExternalApiCircuitBreaker.CODE_LAYER_CIRCUIT_AUTO_REENABLED)
                        .message("Circuit cooldown elapsed — layer re-enabled for autosync")
                        .context(Map.of(
                                "reenableAfterHours", String.valueOf(cooldown.toHours())
                        ))
                        .build());
            } catch (RuntimeException e) {
                log.warn("Circuit auto-re-enable failed for {}: {}", layer, e.getMessage());
            }
        }
    }
}
