package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class Soccer365HttpClient {

    private static final Logger log = LoggerFactory.getLogger(Soccer365HttpClient.class);

    private final Soccer365Properties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);

    public String fetchScheduleHtml(int competitionId) {
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String competitionUrl = base + "/competitions/" + competitionId + "/";
        String scheduleUrl = base + "/competitions/" + competitionId + "/shedule/";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(scheduleUrl, competitionUrl);
    }

    public String fetchGameHtml(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new BadRequestException("soccer365GameIdRequired");
        }
        String id = gameId.trim();
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String gameUrl = base + "/games/" + id + "/";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(gameUrl, gameUrl);
    }

    private void warmHomepage(String base) {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
            getHtmlInternal(base + "/", null, false);
        } catch (RuntimeException e) {
            // Allow first real request to surface the failure; keep warm flag so we do not loop.
            log.info("soccer365 homepage warm-up failed: {}", e.getMessage());
        }
    }

    private String getHtml(String url, String referer) {
        return getHtmlInternal(url, referer, true);
    }

    private String getHtmlInternal(String url, String referer, boolean reportCircuit) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeAccessWall(body)) {
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layerForUrl(url), "soccer365.ru", ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("soccer365FetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("soccer365 HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layerForUrl(url), "soccer365.ru", kind, "HTTP " + response.statusCode());
                }
                throw ExternalApiHttpFailures.fetchFailed("soccer365FetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(layerForUrl(url));
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("soccer365 fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(layerForUrl(url), "soccer365.ru", kind, e.getMessage());
            }
            throw ExternalApiHttpFailures.fetchFailed("soccer365FetchFailed");
        }
    }

    private static ExternalDataLayer layerForUrl(String url) {
        return url != null && url.contains("/games/") ? ExternalDataLayer.FULL_MATCH : ExternalDataLayer.SCHEDULE;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://soccer365.ru";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
