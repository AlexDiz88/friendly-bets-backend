package net.friendly_bets.fifa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "fifa")
public class FifaProperties {

    private String baseUrl = "https://api.fifa.com";
    private String competitionId = "17";
    private String seasonId = "285023";
    /** IdStage группового этапа (First Stage) — для /calendar/.../Standing. */
    private String stageId = "289273";
    private int connectTimeoutMs = 8000;
    private int readTimeoutMs = 15000;
    private long cacheTtlMs = 90_000;
    private String userAgent = "FriendlyBets/1.0 (+https://friendly-bets.net)";
}
