package net.friendly_bets.aiscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "aiscore")
public class AiscoreProperties {

    private String baseUrl = "https://www.aiscore.com";
    private long scheduleSyncIntervalMs = 28_800_000L;
    private long schedulerTickMs = 300_000L;
    private long leagueStaggerMs = 2_100_000L;
    private int syncJitterMinutes = 15;
    private long httpDelayMinMs = 1_500L;
    private long httpDelayMaxMs = 4_500L;
    private int skipWhenKickoffFartherThanDays = 60;
    /**
     * Path after baseUrl without leading slash, e.g.
     * {@code tournament-english-premier-league/mo07dni2vfxknxy}.
     * Schedule URL = {@code {baseUrl}/{path}/schedule}.
     */
    private Map<String, String> tournamentPaths = new HashMap<>(Map.of(
            "EPL", "tournament-english-premier-league/mo07dni2vfxknxy",
            "BL", "tournament-german-bundesliga/1edq09ignayqxgo",
            "CL", "tournament-uefa-champions-league/xo17pji02i37jw5",
            "LE", "tournament-uefa-europa-league/2jr7owi6es1q0em"
    ));
    private List<String> enabledLeagues = new ArrayList<>(List.of("EPL", "BL", "CL", "LE"));
}
