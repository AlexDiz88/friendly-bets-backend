package net.friendly_bets.flashscoreua;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscoreua.config.FlashscoreUaProperties;
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
public class FlashscoreUaHttpClient {

    private static final Logger log = LoggerFactory.getLogger(FlashscoreUaHttpClient.class);

    private final FlashscoreUaProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(30));

    public String fetchOverallTableFeed(FlashscoreUaProperties.LeagueConfig league) {
        if (league == null
                || league.getTournamentId() == null || league.getTournamentId().isBlank()
                || league.getStageId() == null || league.getStageId().isBlank()) {
            throw new BadRequestException("flashscoreUaStandingsNotConfigured");
        }
        String tableId = properties.getOverallTableId() == null || properties.getOverallTableId().isBlank()
                ? "1"
                : properties.getOverallTableId().trim();
        String path = "/x/feed/to_" + league.getTournamentId().trim()
                + "_" + league.getStageId().trim()
                + "_" + tableId;
        String base = trimTrailingSlash(properties.getFeedBaseUrl(), "https://www.flashscore.com.ua");
        String url = base + path;
        String referer = trimTrailingSlash(properties.getBaseUrl(), "https://www.flashscore.com.ua") + "/football/";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getText(url, referer);
    }

    private String getText(String url, String referer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            builder.setHeader("x-fsign", properties.getFeedSign());
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeAccessWall(body)) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.STANDINGS,
                        ExternalProviderIds.FLASHSCORE_UA,
                        ScrapeFailureKind.CHALLENGE,
                        url
                );
                throw ExternalApiHttpFailures.fetchFailed("flashscoreUaFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("flashscore.com.ua HTTP {} for {}", response.statusCode(), url);
                circuitBreaker.recordFailure(
                        ExternalDataLayer.STANDINGS,
                        ExternalProviderIds.FLASHSCORE_UA,
                        kind,
                        "HTTP " + response.statusCode()
                );
                throw ExternalApiHttpFailures.fetchFailed("flashscoreUaFetchFailed");
            }
            if (body.length() < 5 && !"0".equals(body.trim())) {
                log.warn("flashscore.com.ua empty standings feed for {}", url);
                circuitBreaker.recordFailure(
                        ExternalDataLayer.STANDINGS,
                        ExternalProviderIds.FLASHSCORE_UA,
                        ScrapeFailureKind.PARSE_ERROR,
                        url
                );
                throw ExternalApiHttpFailures.fetchFailed("flashscoreUaFetchFailed");
            }
            circuitBreaker.recordSuccess(ExternalDataLayer.STANDINGS);
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("flashscore.com.ua fetch failed for {}: {}", url, e.getMessage());
            circuitBreaker.recordFailure(
                    ExternalDataLayer.STANDINGS,
                    ExternalProviderIds.FLASHSCORE_UA,
                    kind,
                    e.getMessage()
            );
            throw ExternalApiHttpFailures.fetchFailed("flashscoreUaFetchFailed");
        }
    }

    private static String trimTrailingSlash(String baseUrl, String fallback) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return fallback;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
