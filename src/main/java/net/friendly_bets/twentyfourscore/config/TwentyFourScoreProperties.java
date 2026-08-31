package net.friendly_bets.twentyfourscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "twentyfourscore")
public class TwentyFourScoreProperties {

    private String baseUrl = "https://24score.pro";
    private long httpDelayMinMs = 1_500L;
    private long httpDelayMaxMs = 4_000L;
    /** Poll interval while matches are in LIVE window (legacy; prefer external-data.layers.LIVE.scheduler-tick-ms). */
    private long schedulerTickMs = 300_000L;
    /**
     * Team-names page path per league code; use {@code {season}} for {@code 2026-2027}-style segment.
     * EPL/BL: standings; CL/LE main round: fixtures (same AJAX loader as standings).
     */
    private Map<String, String> standingsPaths = new HashMap<>(Map.of(
            "EPL", "/football/england/premier_league/{season}/regular_season/standings/",
            "BL", "/football/germany/1_bundesliga/{season}/regular_season/standings/",
            "CL", "/football/international/uefa_champions_league/{season}/main_round/fixtures/",
            "LE", "/football/international/uefa_europa_league/{season}/main_round/fixtures/"
    ));
}
