package net.friendly_bets.twentyfourscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "twentyfourscore")
public class TwentyFourScoreProperties {

    private String baseUrl = "https://24score.pro";
    private long httpDelayMinMs = 1_000L;
    private long httpDelayMaxMs = 3_000L;
    /** How often LIVE scheduler ticks (default 5 min). */
    private long schedulerTickMs = 300_000L;
    /**
     * Standings page path per league code; use {@code {season}} for {@code 2026-2027}-style segment.
     * CL/LE omitted until qualification ends.
     */
    private Map<String, String> standingsPaths = new HashMap<>(Map.of(
            "EPL", "/football/england/premier_league/{season}/regular_season/standings/",
            "BL", "/football/germany/1_bundesliga/{season}/regular_season/standings/"
    ));
}
