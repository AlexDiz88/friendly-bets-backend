package net.friendly_bets.scrape;

/**
 * Classifies outbound scrape failures for circuit-breaker decisions.
 */
public enum ScrapeFailureKind {
    /** TCP/TLS connect or read timeout. */
    TIMEOUT,
    /** DNS/connect/reset and other transport failures. */
    NETWORK_ERROR,
    /** HTTP 403 / 429 / 503 and similar soft blocks. */
    HTTP_BLOCKED,
    /** Cloudflare (or similar) JS challenge HTML. */
    CHALLENGE,
    /** Other HTTP 4xx/5xx that should not trip the breaker alone. */
    HTTP_ERROR,
    /** Response received but body unusable. */
    PARSE_ERROR
}
