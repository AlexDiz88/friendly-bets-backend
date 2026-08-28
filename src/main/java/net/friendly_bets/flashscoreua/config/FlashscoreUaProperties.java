package net.friendly_bets.flashscoreua.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "flashscoreua")
public class FlashscoreUaProperties {

    private String baseUrl = "https://www.flashscore.com.ua";
    /** Site origin {@code /x/feed/} — localized legend. Ninja {@code /2/x/feed/} returns English TV labels. */
    private String feedBaseUrl = "https://www.flashscore.com.ua";
    private String feedSign = "SW9D1eZo";
    /** Overall table id in {@code to_{tournamentId}_{stageId}_{tableId}}. */
    private String overallTableId = "1";
    private long httpDelayMinMs = 800L;
    private long httpDelayMaxMs = 2_000L;
    /** League code → tournament page / feed ids (EPL, BL; CL/LE league stage). */
    private Map<String, LeagueConfig> leagues = new HashMap<>(Map.of(
            "EPL", league("/football/england/premier-league/", "SY30SsKF", "CfoA8Dmm"),
            "BL", league("/football/germany/bundesliga/", "KY7LrA6d", "jg0MwVuC"),
            "CL", league("/football/europe/champions-league/", "tfRdlhP9", "vT1iNRGq"),
            "LE", league("/football/europe/europa-league/", "nTFvUo9N", "OjQOTQKa")
    ));

    private static LeagueConfig league(String tournamentPath, String tournamentId, String stageId) {
        LeagueConfig c = new LeagueConfig();
        c.setTournamentPath(tournamentPath);
        c.setTournamentId(tournamentId);
        c.setStageId(stageId);
        return c;
    }

    @Data
    public static class LeagueConfig {
        private String tournamentPath;
        private String tournamentId;
        private String stageId;
    }
}
