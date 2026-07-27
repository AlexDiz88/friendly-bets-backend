package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSiteAccessProbeResultDto {
    /**
     * PASS — HTTP 2xx/3xx without JS challenge (Java likely OK from this host).
     * CLOUDFLARE_JS_CHALLENGE — Cloudflare interstitial (same class as aiscore).
     * HTTP_BLOCKED — HTTP status ≥ 400 without clear JS challenge.
     * NETWORK_ERROR — DNS/TLS/timeout/connect failure.
     */
    private String verdict;
    private String requestedUrl;
    private String finalUrl;
    private Integer httpStatus;
    private Long durationMs;
    private String serverHeader;
    private String cfRay;
    private String cfMitigated;
    private boolean cloudflareDetected;
    private boolean jsChallengeSuspected;
    /** Truncated response body for diagnostics. */
    private String bodySnippet;
    private String errorDetail;
}
