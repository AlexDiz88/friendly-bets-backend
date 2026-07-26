package net.friendly_bets.soccer365.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "soccer365")
public class Soccer365Properties {

    private String baseUrl = "https://soccer365.ru";
    /** Interval between schedule syncs per league (default 8h). */
    private long scheduleSyncIntervalMs = 28_800_000L;
    /** How often the scheduler checks due leagues (default 5 min). */
    private long schedulerTickMs = 300_000L;
    /** Stagger between leagues on first due window (default 35 min). */
    private long leagueStaggerMs = 2_100_000L;
    /** Jitter up to ±N minutes on due check. */
    private int syncJitterMinutes = 15;
    /** Random pause lower bound between HTTP requests (ms). */
    private long httpDelayMinMs = 1_500L;
    /** Random pause upper bound between HTTP requests (ms). */
    private long httpDelayMaxMs = 4_500L;
    /**
     * If the earliest kickoff in the sync window is farther than this many days away,
     * skip the tick (rare-schedule mode).
     */
    private int skipWhenKickoffFartherThanDays = 3;
    private Map<String, Integer> competitionIds = new HashMap<>(Map.of(
            "EPL", 12,
            "BL", 17,
            "CL", 19,
            "LE", 20,
            "EC", 24,
            "WC", 742
    ));
    private List<String> enabledLeagues = new ArrayList<>(List.of("EPL", "BL", "CL", "LE"));
}
