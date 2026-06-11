package net.friendly_bets.marathonbet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "marathonbet")
public class MarathonbetProperties {

    private boolean syncEnabled = true;
    /** Interval between ticks for each slot (current / next). Default 6 hours. */
    private long slotSyncIntervalMs = 21_600_000L;
    /** Offset before the first next-slot tick (default 3 hours after current). */
    private long slotSyncStaggerMs = 10_800_000L;
    /** Legacy standalone scheduler: both slots in one tick. */
    private long syncIntervalMs = 43_200_000L;
    /** Jitter up to ±15 minutes on scheduled tick. */
    private int syncJitterMinutes = 15;
    private Map<String, Long> tournamentTreeIds = new HashMap<>(Map.of("WC", 2_253_726L));
    private List<String> primaryForLeagues = new ArrayList<>(List.of("WC"));
    private boolean fallbackToOddsApi = true;
    private int eventWindowHours = 6;
    /** Random pause lower bound between per-event SSE fetches (ms). */
    private long sseDelayMinMs = 1_500L;
    /** Random pause upper bound between per-event SSE fetches (ms). */
    private long sseDelayMaxMs = 4_500L;
}
