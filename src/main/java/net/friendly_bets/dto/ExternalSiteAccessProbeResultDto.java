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
     * PASS — HTTP 2xx/3xx without an access wall (Java likely OK from this host).
     * CLOUDFLARE_JS_CHALLENGE — Cloudflare interstitial (same class as aiscore).
     * AUTH_INTERSTITIAL — SSO/login HTML (e.g. championat.com SberID) instead of the page.
     * GRAPHQL_NEEDS_QUERY — GraphQL engine reachable; GET/empty body is not a CF block.
     * HTTP_BLOCKED — HTTP status ≥ 400 without a clear wall.
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
    private Integer bodyLength;
    /** Visible text / JSON extract (HTML tags stripped). */
    private String bodySnippet;
    private String errorDetail;
}
