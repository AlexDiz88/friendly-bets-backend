package net.friendly_bets.flashscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "flashscore")
public class FlashscoreProperties {

    private String baseUrl = "https://www.flashscorekz.com";
    /** English tournament pages for admin team-alias sync (FH/FK labels). */
    private String teamNamesBaseUrl = "https://www.flashscore.com";
    private String feedBaseUrl = "https://46.flashscore.ninja";
    private String feedSign = "SW9D1eZo";
    private String feedLocale = "ru-kz";
    /**
     * IANA zone of this Flashscore edition's day calendar ({@code f_1_{offset}} where 0 = today).
     * kz edition follows Asia/Almaty, not UTC.
     */
    private String feedTimezone = "Asia/Almaty";
    /** Kickoff ±window hours when resolving FULL_MATCH via day feeds. */
    private int fullMatchKickoffWindowHours = 12;
    private long httpDelayMinMs = 800L;
    private long httpDelayMaxMs = 2_000L;
    /** League code → tournament page / filter metadata (EPL, BL; CL/LE league stage). */
    private Map<String, LeagueConfig> leagues = new HashMap<>(Map.of(
            "EPL", league("/football/england/premier-league/", "CfoA8Dmm", "Premier League"),
            "BL", league("/football/germany/bundesliga/", "jg0MwVuC", "Bundesliga"),
            "CL", league("/football/europe/champions-league/", "vT1iNRGq", "champions-league"),
            "LE", league("/football/europe/europa-league/", "OjQOTQKa", "europa-league")
    ));

    private static LeagueConfig league(String tournamentPath, String stageId, String titleContains) {
        LeagueConfig c = new LeagueConfig();
        c.setTournamentPath(tournamentPath);
        c.setStageId(stageId);
        c.setTitleContains(titleContains);
        return c;
    }

    public ZoneId feedZone() {
        if (feedTimezone == null || feedTimezone.isBlank()) {
            throw new IllegalStateException("flashscore.feed-timezone is required");
        }
        return ZoneId.of(feedTimezone.trim());
    }

    @Data
    public static class LeagueConfig {
        private String tournamentPath;
        private String stageId;
        private String titleContains;
    }
}
