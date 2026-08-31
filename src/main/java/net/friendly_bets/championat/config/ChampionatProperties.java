package net.friendly_bets.championat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "championat")
public class ChampionatProperties {

    private String baseUrl = "https://www.championat.com";
    private long httpDelayMinMs = 800L;
    private long httpDelayMaxMs = 2_000L;
    /**
     * Tournament numeric ids on championat.com (season-specific; update each season).
     * Used to filter LIVE date JSON and build standings URLs when path is absent.
     */
    private Map<String, Integer> tournamentIds = new HashMap<>(Map.of(
            "EPL", 7142,
            "BL", 7146,
            "CL", 7100,
            "LE", 7104
    ));
    /**
     * Tournament page path per league (trailing slash). Standings: {@code path + "table/"}.
     */
    private Map<String, String> tournamentPaths = new HashMap<>(Map.of(
            "EPL", "/football/_england/tournament/7142/",
            "BL", "/football/_germany/tournament/7146/",
            "CL", "/football/_ucl/tournament/7100/",
            "LE", "/football/_europeleague/tournament/7104/"
    ));
}
