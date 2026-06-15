package net.friendly_bets.twentyfourscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "twentyfourscore")
public class TwentyFourScoreProperties {

    private boolean enabled = true;
    private String baseUrl = "https://24score.pro";
    private String userAgent = "FriendlyBets/1.0";
    private long requestDelayMs = 1500L;
    private long pollingIntervalMs = 300_000L;
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 30000;
    private List<String> secondaryForLeagues = new ArrayList<>(List.of("WC"));
}
