package net.friendly_bets.football24.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "football24")
public class Football24Properties {

    private String apiBaseUrl = "https://api.football24.ua";
    private String siteBaseUrl = "https://football24.ua";
    private String scheduleZone = "Europe/Berlin";
    private int leagueHourStep = 2;
    /** Stagger after soccer365 (base 1) and sports.ru (base 2). */
    private int leagueHourBase = 3;
    private int syncJitterMinutes = 15;
    private long httpDelayMinMs = 1_500L;
    private long httpDelayMaxMs = 4_500L;
    /**
     * If DB already has current-matchday rows and earliest kickoff is farther than this many days,
     * skip the HTTP request (auto sync only).
     */
    private int skipWhenKickoffFartherThanDays = 3;
    /**
     * Internal football24 league ids for {@code /season/getSeasonsByLeagueId} and fixtures.
     * Distinct from public tournament slug tag ids (e.g. anglija-50820).
     */
    private Map<String, Integer> leagueIds = new HashMap<>(Map.of(
            "EPL", 3,
            "BL", 7,
            "CL", 1,
            "LE", 2
    ));
    /**
     * Public tournament slugs (path segment) for referer / admin docs.
     * EC/WC to be added later.
     */
    private Map<String, String> tournamentSlugs = new HashMap<>(Map.of(
            "EPL", "anglija-50820",
            "BL", "germanija-50829",
            "CL", "liga-chempionov-50824",
            "LE", "liga-evropy-50821"
    ));
    private Set<String> scheduleSyncLeagueCodes = new LinkedHashSet<>(Set.of("EPL", "BL", "CL", "LE"));
}
