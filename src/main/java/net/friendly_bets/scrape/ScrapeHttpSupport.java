package net.friendly_bets.scrape;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Shared builders and browser-like header application for HTML scrapers.
 */
public final class ScrapeHttpSupport {

    private ScrapeHttpSupport() {
    }

    public static HttpClient newBrowserClient(Duration connectTimeout) {
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout != null ? connectTimeout : Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookies)
                .build();
    }

    /**
     * Navigation-style GET (HTML page).
     */
    public static void applyNavigationHeaders(
            HttpRequest.Builder builder,
            BrowserProfile profile,
            String referer
    ) {
        applyCommon(builder, profile);
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", referer != null && !referer.isBlank() ? "same-origin" : "none")
                .header("Sec-Fetch-User", "?1");
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
    }

    /**
     * XHR / fetch-style GET (AJAX data endpoints).
     */
    public static void applyXhrHeaders(
            HttpRequest.Builder builder,
            BrowserProfile profile,
            String referer
    ) {
        applyCommon(builder, profile);
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin");
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
    }

    private static void applyCommon(HttpRequest.Builder builder, BrowserProfile profile) {
        builder.header("User-Agent", profile.userAgent())
                .header("Accept-Language", profile.acceptLanguage())
                .header("sec-ch-ua", profile.secChUa())
                .header("sec-ch-ua-mobile", profile.secChUaMobile())
                .header("sec-ch-ua-platform", profile.secChUaPlatform());
    }

    /**
     * Sleep between {@code minMs} and {@code maxMs}, with occasional longer human-like pause.
     */
    public static void jitterSleep(long minMs, long maxMs) {
        long min = Math.max(0L, minMs);
        long max = Math.max(min, maxMs);
        long delay = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        // ~12% chance of an extra 1–3s pause (human hesitation)
        if (delay > 0 && ThreadLocalRandom.current().nextInt(100) < 12) {
            delay += ThreadLocalRandom.current().nextLong(1_000L, 3_001L);
        }
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cloudflare (or similar) JS challenge HTML — Java HttpClient cannot complete it.
     */
    public static boolean looksLikeJsChallenge(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("just a moment")
                || lower.contains("cf-challenge")
                || lower.contains("challenge-platform")
                || lower.contains("checking your browser")
                || lower.contains("enable javascript and cookies")
                || (lower.contains("attention required") && lower.contains("cloudflare"));
    }

    /**
     * Login/SSO interstitial served as HTTP 200 HTML instead of the requested page
     * (championat.com SberID / Unity ID since 2026-08). Not a real scores/JSON body.
     */
    public static boolean looksLikeAuthInterstitial(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("авторизация sberid")
                || lower.contains("/static/auth/_script")
                || (lower.contains("вход через sberid") && lower.contains("перенаправление"));
    }

    /** Any HTML wall that must not be parsed as provider data (CF challenge or SSO interstitial). */
    public static boolean looksLikeAccessWall(String body) {
        return looksLikeJsChallenge(body) || looksLikeAuthInterstitial(body);
    }

    public static ScrapeFailureKind classifyHttpStatus(int status) {
        if (status == 403 || status == 429 || status == 503) {
            return ScrapeFailureKind.HTTP_BLOCKED;
        }
        return ScrapeFailureKind.HTTP_ERROR;
    }

    public static ScrapeFailureKind classifyThrowable(Throwable e) {
        if (e == null) {
            return ScrapeFailureKind.NETWORK_ERROR;
        }
        String name = e.getClass().getName();
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase(Locale.ROOT) : "";
        if (name.contains("HttpTimeoutException")
                || msg.contains("timed out")
                || msg.contains("timeout")) {
            return ScrapeFailureKind.TIMEOUT;
        }
        return ScrapeFailureKind.NETWORK_ERROR;
    }

    /**
     * Decode HTTP body: gunzip/deflate when {@code Content-Encoding} (or gzip magic) says so,
     * then charset from {@code Content-Type} (default UTF-8).
     * Java {@link HttpClient} does not auto-decompress when the server still sends gzip.
     */
    public static String decodeResponseBody(byte[] body, HttpHeaders headers) {
        if (body == null || body.length == 0) {
            return "";
        }
        String encoding = headerValue(headers, "Content-Encoding").orElse(null);
        String contentType = headerValue(headers, "Content-Type").orElse(null);
        byte[] raw = decompressIfNeeded(body, encoding);
        return new String(raw, charsetFromContentType(contentType));
    }

    static byte[] decompressIfNeeded(byte[] body, String contentEncoding) {
        if (body == null || body.length == 0) {
            return body != null ? body : new byte[0];
        }
        String enc = contentEncoding != null ? contentEncoding.toLowerCase(Locale.ROOT).trim() : "";
        boolean gzipHeader = enc.contains("gzip");
        boolean deflateHeader = enc.contains("deflate");
        boolean gzipMagic = body.length >= 2
                && (body[0] & 0xFF) == 0x1F
                && (body[1] & 0xFF) == 0x8B;
        if (gzipHeader || gzipMagic) {
            try {
                return readCompressed(new GZIPInputStream(new ByteArrayInputStream(body)));
            } catch (IOException e) {
                if (gzipHeader) {
                    throw new UncheckedIOException("gzip decompress failed", e);
                }
                return body;
            }
        }
        if (deflateHeader) {
            try {
                return readCompressed(new InflaterInputStream(new ByteArrayInputStream(body)));
            } catch (IOException e) {
                throw new UncheckedIOException("deflate decompress failed", e);
            }
        }
        return body;
    }

    static Charset charsetFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("charset=");
        if (idx < 0) {
            return StandardCharsets.UTF_8;
        }
        String raw = contentType.substring(idx + "charset=".length()).trim();
        int semi = raw.indexOf(';');
        if (semi >= 0) {
            raw = raw.substring(0, semi).trim();
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1);
        }
        try {
            return Charset.forName(raw);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static byte[] readCompressed(InputStream in) throws IOException {
        try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            stream.transferTo(out);
            return out.toByteArray();
        }
    }

    private static Optional<String> headerValue(HttpHeaders headers, String name) {
        if (headers == null || name == null) {
            return Optional.empty();
        }
        return headers.firstValue(name);
    }
}
