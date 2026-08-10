package net.friendly_bets.ruscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ruscore")
public class RuscoreProperties {

    private String baseUrl = "https://ruscore.ru";
    /** Kickoff ±window hours when resolving FULL_MATCH via day pages. */
    private int fullMatchKickoffWindowHours = 12;
    private long httpDelayMinMs = 800L;
    private long httpDelayMaxMs = 2_000L;
    /** Season page ids for alias auto-bind (leagueCode → seasonId). */
    private Map<String, Integer> seasonIds = new HashMap<>(Map.of(
            "EPL", 5379,
            "BL", 5456,
            "CL", 5358,
            "LE", 5360
    ));
    /** Tournament URL slugs (leagueCode → slug); used with seasonIds for calendar. */
    private Map<String, String> tournamentSlugs = new HashMap<>(Map.of(
            "EPL", "england-premier-league",
            "BL", "germany-bundesliga",
            "CL", "uefa-champions-league",
            "LE", "uefa-europa-league"
    ));
}
