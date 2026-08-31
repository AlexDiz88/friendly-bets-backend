package net.friendly_bets.eurofootball;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.eurofootball.config.EuroFootballProperties;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EuroFootballHttpClient {

    private static final Logger log = LoggerFactory.getLogger(EuroFootballHttpClient.class);

    private final EuroFootballProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));

    /**
     * Current-day LIVE JSON. Source ignores {@code ?date=} — always the site's "today" catalogue.
     */
    public String fetchLiveJson() {
        String base = trimTrailingSlash(properties.getBaseUrl());
        String url = base + "/online/data";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getBody(url, base + "/online/today", true);
    }

    public String fetchLeagueTablesHtml(String leaguePath) {
        return fetchLeaguePageHtml(leaguePath, "tables");
    }

    public String fetchLeagueCalendarHtml(String leaguePath) {
        return fetchLeaguePageHtml(leaguePath, "calendar");
    }

    private String fetchLeaguePageHtml(String leaguePath, String pageSuffix) {
        if (leaguePath == null || leaguePath.isBlank()) {
            throw new BadRequestException("euroFootballLeagueNotSupported");
        }
        if (pageSuffix == null || pageSuffix.isBlank()) {
            throw new BadRequestException("euroFootballLeagueNotSupported");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String path = leaguePath.startsWith("/") ? leaguePath : "/" + leaguePath;
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String suffix = pageSuffix.startsWith("/") ? pageSuffix.substring(1) : pageSuffix;
        String url = base + path + "/" + suffix;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getBody(url, base + "/", false);
    }

    private String getBody(String url, String referer, boolean expectJson) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET();
            if (expectJson) {
                ScrapeHttpSupport.applyXhrHeaders(builder, browserProfile, referer);
                builder.header("Accept", "application/json, text/plain, */*");
            } else {
                ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeAccessWall(body)) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.LIVE, ExternalProviderIds.EURO_FOOTBALL, ScrapeFailureKind.CHALLENGE, url);
                throw ExternalApiHttpFailures.fetchFailed("euroFootballFetchFailed");
            }
            if (expectJson && looksLikeHtml(body)) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.LIVE, ExternalProviderIds.EURO_FOOTBALL, ScrapeFailureKind.HTTP_ERROR, url);
                throw ExternalApiHttpFailures.fetchFailed("euroFootballFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("euro-football HTTP {} for {}", response.statusCode(), url);
                circuitBreaker.recordFailure(
                        ExternalDataLayer.LIVE, ExternalProviderIds.EURO_FOOTBALL, kind, "HTTP " + response.statusCode());
                throw ExternalApiHttpFailures.fetchFailed("euroFootballFetchFailed");
            }
            circuitBreaker.recordSuccess(ExternalDataLayer.LIVE);
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("euro-football fetch failed for {}: {}", url, e.getMessage());
            circuitBreaker.recordFailure(
                    ExternalDataLayer.LIVE, ExternalProviderIds.EURO_FOOTBALL, kind, e.getMessage());
            throw ExternalApiHttpFailures.fetchFailed("euroFootballFetchFailed");
        }
    }

    private static boolean looksLikeHtml(String body) {
        if (body == null) {
            return false;
        }
        String trimmed = body.stripLeading();
        return trimmed.startsWith("<") || trimmed.regionMatches(true, 0, "<!doctype", 0, 9);
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.euro-football.ru";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
