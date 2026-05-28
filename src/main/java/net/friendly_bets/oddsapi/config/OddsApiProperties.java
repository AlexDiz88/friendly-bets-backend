package net.friendly_bets.oddsapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "odds-api")
public class OddsApiProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.odds-api.io/v3";
    private boolean syncEnabled = true;
    /** 12 hours by default. */
    private long syncIntervalMs = 43_200_000L;
    private List<String> bookmakers = new ArrayList<>(List.of("Bet365", "1xbet"));
    /**
     * Override {@link net.friendly_bets.oddsapi.OddsApiLeagueMapping} defaults, e.g.
     * {@code odds-api.league-slugs.EPL=england-premier-league}.
     */
    private Map<String, String> leagueSlugs = new HashMap<>();
    /** Hours before/after kick-off when fetching events for matching. */
    private int eventWindowHours = 6;
}
