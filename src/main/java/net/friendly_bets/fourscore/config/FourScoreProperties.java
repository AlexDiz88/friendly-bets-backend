package net.friendly_bets.fourscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "fourscore")
public class FourScoreProperties {

    private boolean enabled = true;
    private String baseUrl = "https://4score.ru";
    private String userAgent = "FriendlyBets/1.0";
    private long requestDelayMs = 1500L;
    /** Throttle between HTTP calls in admin preview (many event pages per request). */
    private long previewRequestDelayMs = 300L;
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 30000;
    private List<String> primaryForLeagues = new ArrayList<>(List.of("WC"));
}
