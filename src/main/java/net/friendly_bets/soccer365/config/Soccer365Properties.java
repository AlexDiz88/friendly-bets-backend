package net.friendly_bets.soccer365.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "soccer365")
public class Soccer365Properties {

    private String baseUrl = "https://soccer365.ru";
    /** Zone for per-league schedule hours (same idea as marathonbet). */
    private String scheduleZone = "Europe/Berlin";
    /**
     * Hour step between leagues in the active season (league index × step).
     * League 0 → hours 1 and 13; league 1 → 3 and 15; …
     */
    private int leagueHourStep = 2;
    /** Base hour of day for the first league's first slot (0–23). */
    private int leagueHourBase = 1;
    /** Random delay 0..N minutes after a due hour before syncing. */
    private int syncJitterMinutes = 15;
    /** Random pause lower bound between HTTP requests (ms). */
    private long httpDelayMinMs = 1_500L;
    /** Random pause upper bound between HTTP requests (ms). */
    private long httpDelayMaxMs = 4_500L;
    /**
     * If DB already has current-matchday rows and earliest kickoff is farther than this many days,
     * skip the HTTP request (auto sync only).
     */
    private int skipWhenKickoffFartherThanDays = 3;
    /** Kickoff±window when resolving FULL_MATCH card via competition schedule. */
    private int fullMatchKickoffWindowMinutes = 30;
    private Map<String, Integer> competitionIds = new HashMap<>(Map.of(
            "EPL", 12,
            "BL", 17,
            "CL", 19,
            "LE", 20,
            "EC", 24,
            "WC", 742
    ));
}
