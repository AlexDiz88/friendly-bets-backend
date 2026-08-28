package net.friendly_bets.liveresult.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "liveresult")
public class LiveresultProperties {

    private String baseUrl = "https://www.liveresult.ru";
    private long httpDelayMinMs = 1_500L;
    private long httpDelayMaxMs = 4_000L;
    /** Standings page path per league code (absolute path on liveresult.ru). */
    private Map<String, String> standingsPaths = new HashMap<>(Map.of(
            "EPL", "/football/England/Premier-League/standings",
            "BL", "/football/Germany/Bundesliga-I/standings",
            "CL", "/football/Champions-League",
            "LE", "/football/Europa-League"
    ));
}
