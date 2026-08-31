package net.friendly_bets.sportsru.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "sportsru")
public class SportsRuProperties {

    private String baseUrl = "https://www.sports.ru";
    private String scheduleZone = "Europe/Berlin";
    private int leagueHourStep = 2;
    private int leagueHourBase = 2;
    private int syncJitterMinutes = 15;
    private long httpDelayMinMs = 1_500L;
    private long httpDelayMaxMs = 4_500L;
    /**
     * If DB already has current-matchday rows and earliest kickoff is farther than this many days,
     * skip the HTTP request (auto sync only).
     */
    private int skipWhenKickoffFartherThanDays = 3;
    /**
     * Calendar paths per {@link net.friendly_bets.models.League.LeagueCode} (relative to baseUrl).
     * Paths for EPL/BL/CL/LE; enabled sync set is {@link #scheduleSyncLeagueCodes}.
     */
    private Map<String, String> calendarPaths = new HashMap<>(Map.of(
            "EPL", "/football/tournament/premier-league/calendar/",
            "BL", "/football/tournament/bundesliga/calendar/",
            "CL", "/football/tournament/ucl/calendar/",
            "LE", "/football/tournament/uel/calendar/"
    ));
    /** League codes whose calendar HTML is parsed for SCHEDULE. */
    private Set<String> scheduleSyncLeagueCodes = new LinkedHashSet<>(Set.of("EPL", "BL", "CL", "LE"));
}
