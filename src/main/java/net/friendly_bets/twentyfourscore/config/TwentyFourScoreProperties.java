package net.friendly_bets.twentyfourscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "twentyfourscore")
public class TwentyFourScoreProperties {

    private boolean syncEnabled = true;
    private String baseUrl = "https://24score.pro";
    private long httpDelayMinMs = 1_000L;
    private long httpDelayMaxMs = 3_000L;
    /** How often LIVE scheduler ticks (default 5 min). */
    private long schedulerTickMs = 300_000L;
}
