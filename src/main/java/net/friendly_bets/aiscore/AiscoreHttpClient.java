package net.friendly_bets.aiscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.aiscore.config.AiscoreProperties;
import net.friendly_bets.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class AiscoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AiscoreHttpClient.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final AiscoreProperties properties;
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);
    private final AtomicReference<HttpClient> httpClientRef = new AtomicReference<>();

    /**
     * Cloudflare often returns 403 for Java HttpClient over HTTP/2 and for datacenter IPs.
     * HTTP/1.1 + cookie jar + homepage warm-up; optional {@code aiscore.http-proxy} for hosting.
     */
    private HttpClient client() {
        HttpClient existing = httpClientRef.get();
        if (existing != null) {
            return existing;
        }
        synchronized (httpClientRef) {
            existing = httpClientRef.get();
            if (existing != null) {
                return existing;
            }
            CookieManager cookies = new CookieManager();
            cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .version(HttpClient.Version.HTTP_1_1)
                    .cookieHandler(cookies);
            ProxySelector proxy = resolveProxy(properties.getHttpProxy());
            if (proxy != null) {
                builder.proxy(proxy);
                log.info("aiscore HTTP client using proxy {}", properties.getHttpProxy());
            }
            HttpClient created = builder.build();
            httpClientRef.set(created);
            return created;
        }
    }

    public String fetchScheduleHtml(String tournamentPath) {
        if (tournamentPath == null || tournamentPath.isBlank()) {
            throw new BadRequestException("aiscoreTournamentNotConfigured");
        }
        String path = tournamentPath.trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.endsWith("/schedule")) {
            path = path.substring(0, path.length() - "/schedule".length());
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String tournamentUrl = base + "/" + path;
        String scheduleUrl = tournamentUrl + "/schedule";
        jitterSleep();
        return getHtml(scheduleUrl, tournamentUrl);
    }

    private void warmHomepage(String base) {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            jitterSleep();
            getHtml(base + "/", "https://www.google.com/");
        } catch (RuntimeException e) {
            homepageWarmed.set(false);
            log.warn("aiscore homepage warm-up failed: {}", e.getMessage());
        }
    }

    private String getHtml(String url, String referer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .GET()
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,"
                                    + "image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("User-Agent", USER_AGENT)
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Ch-Ua",
                            "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", refererContainsSameHost(url, referer) ? "same-origin" : "cross-site")
                    .header("Sec-Fetch-User", "?1");
            if (referer != null && !referer.isBlank()) {
                builder.header("Referer", referer);
            }
            HttpResponse<String> response = client().send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                String cfRay = response.headers().firstValue("cf-ray").orElse("-");
                String server = response.headers().firstValue("server").orElse("-");
                String body = response.body() != null ? response.body() : "";
                String snippet = body.length() > 180 ? body.substring(0, 180).replace('\n', ' ') : body.replace('\n', ' ');
                log.warn("aiscore HTTP {} ({}) server={} cf-ray={} for {} bodySnippet={}",
                        response.statusCode(), response.version(), server, cfRay, url, snippet);
                throw new BadRequestException("aiscoreFetchFailed");
            }
            return response.body() != null ? response.body() : "";
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("aiscore fetch failed for {}: {}", url, e.getMessage());
            throw new BadRequestException("aiscoreFetchFailed");
        }
    }

    void jitterSleep() {
        long min = Math.max(0L, properties.getHttpDelayMinMs());
        long max = Math.max(min, properties.getHttpDelayMaxMs());
        long delay = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean refererContainsSameHost(String url, String referer) {
        if (url == null || referer == null) {
            return false;
        }
        try {
            String host = URI.create(url).getHost();
            String refHost = URI.create(referer).getHost();
            return host != null && host.equalsIgnoreCase(refHost);
        } catch (Exception e) {
            return false;
        }
    }

    private static ProxySelector resolveProxy(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || port <= 0) {
                log.warn("aiscore.http-proxy ignored (need host:port): {}", raw);
                return null;
            }
            return ProxySelector.of(new InetSocketAddress(host, port));
        } catch (Exception e) {
            log.warn("aiscore.http-proxy invalid: {}", e.getMessage());
            return null;
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.aiscore.com";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
