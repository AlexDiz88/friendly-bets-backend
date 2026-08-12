package net.friendly_bets.sportsru;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import net.friendly_bets.sportsru.config.SportsRuProperties;
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
public class SportsRuHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SportsRuHttpClient.class);

    private final SportsRuProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);

    public String fetchCalendarHtml(String calendarPath) {
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String path = normalizePath(calendarPath);
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/");
    }

    public String fetchMatchHtml(String matchPath) {
        if (matchPath == null || matchPath.isBlank()) {
            throw new BadRequestException("sportsRuFetchFailed");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String path = normalizePath(matchPath);
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/");
    }

    private void warmHomepage(String base) {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
            getHtmlInternal(base + "/", null, false);
        } catch (RuntimeException e) {
            log.info("sports.ru homepage warm-up failed: {}", e.getMessage());
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
            if (ScrapeHttpSupport.looksLikeJsChallenge(body)) {
                if (reportCircuit) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.SCHEDULE, ExternalProviderIds.SPORTS_RU, ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("sportsRuFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("sports.ru HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.SCHEDULE, ExternalProviderIds.SPORTS_RU, kind, "HTTP " + response.statusCode());
                }
                throw ExternalApiHttpFailures.fetchFailed("sportsRuFetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(ExternalDataLayer.SCHEDULE);
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("sports.ru fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.SCHEDULE, ExternalProviderIds.SPORTS_RU, kind, e.getMessage());
            }
            throw ExternalApiHttpFailures.fetchFailed("sportsRuFetchFailed");
        }
    }

    private static String normalizePath(String path) {
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return trimmed;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.sports.ru";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
