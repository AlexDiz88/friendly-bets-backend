package net.friendly_bets.eurofootball.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "eurofootball")
public class EuroFootballProperties {

    private String baseUrl = "https://www.euro-football.ru";
    private long httpDelayMinMs = 800L;
    private long httpDelayMaxMs = 2_000L;
    /**
     * League overview path (no trailing slash). EPL/BL team names: {@code path + "/tables"};
     * CL/LE: {@code path + "/calendar"} (group tables widget is empty on /tables).
     */
    private Map<String, String> leaguePaths = new HashMap<>(Map.of(
            "EPL", "/online/angliya/premer-liga",
            "BL", "/online/germaniya/bundesliga",
            "CL", "/online/liga_chempionov",
            "LE", "/online/liga_evropyi"
    ));
}
