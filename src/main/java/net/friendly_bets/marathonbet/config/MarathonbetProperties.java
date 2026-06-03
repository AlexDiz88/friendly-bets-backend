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
    /** 12 hours by default. */
    private long syncIntervalMs = 43_200_000L;
    /** Jitter up to ±15 minutes on scheduled tick. */
    private int syncJitterMinutes = 15;
    private Map<String, Long> tournamentTreeIds = new HashMap<>(Map.of("WC", 2_253_726L));
    private List<String> primaryForLeagues = new ArrayList<>(List.of("WC"));
    private boolean fallbackToOddsApi = true;
    /** Min matched/eligible ratio to skip odds-api for WC on the same tick. */
    private double skipOddsApiMinMatchRatio = 0.8;
    private int eventWindowHours = 6;
    /** Pause between per-event SSE fetches (correct score). */
    private long sseDelayMs = 2_000L;
    private boolean fetchCorrectScoreViaSse = true;
    /** Models parsed from tournament JSON without SSE. */
    private List<String> tournamentMarketModels = new ArrayList<>(List.of(
            "MTCH_R", "MTCH_HB", "MTCH_TTLG"
    ));
    /** Extra models loaded from full event SSE when enabled. */
    private List<String> sseExtraMarketModels = new ArrayList<>(List.of(
            "MTCH_CSDYN"
    ));
}
