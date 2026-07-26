package net.friendly_bets.aiscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.aiscore.config.AiscoreProperties;
import net.friendly_bets.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class AiscoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AiscoreHttpClient.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final AiscoreProperties properties;
    /**
     * Cloudflare on aiscore.com returns 403 for Java HttpClient over HTTP/2;
     * HTTP/1.1 with the same headers succeeds.
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

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
        String scheduleUrl = base + "/" + path + "/schedule";
        jitterSleep();
        return getHtml(scheduleUrl, base + "/");
    }

    private String getHtml(String url, String referer) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .GET()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", referer)
                    .header("Upgrade-Insecure-Requests", "1")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("aiscore HTTP {} ({}) for {}", response.statusCode(), response.version(), url);
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
