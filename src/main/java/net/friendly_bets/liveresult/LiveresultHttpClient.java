package net.friendly_bets.liveresult;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.liveresult.config.LiveresultProperties;
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
public class LiveresultHttpClient {

    private static final Logger log = LoggerFactory.getLogger(LiveresultHttpClient.class);

    private final LiveresultProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));

    public String fetchStandingsHtml(String standingsPath) {
        if (standingsPath == null || standingsPath.isBlank()) {
            throw new BadRequestException("liveresultStandingsNotConfigured");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String path = standingsPath.startsWith("/") ? standingsPath : "/" + standingsPath;
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/");
    }

    private String getHtml(String url, String referer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeJsChallenge(body)) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.STANDINGS,
                        ExternalProviderIds.LIVERESULT,
                        ScrapeFailureKind.CHALLENGE,
                        url
                );
                throw ExternalApiHttpFailures.fetchFailed("liveresultFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("liveresult HTTP {} for {}", response.statusCode(), url);
                circuitBreaker.recordFailure(
                        ExternalDataLayer.STANDINGS,
                        ExternalProviderIds.LIVERESULT,
                        kind,
                        "HTTP " + response.statusCode()
                );
                throw ExternalApiHttpFailures.fetchFailed("liveresultFetchFailed");
            }
            circuitBreaker.recordSuccess(ExternalDataLayer.STANDINGS);
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("liveresult fetch failed for {}: {}", url, e.getMessage());
            circuitBreaker.recordFailure(
                    ExternalDataLayer.STANDINGS,
                    ExternalProviderIds.LIVERESULT,
                    kind,
                    e.getMessage()
            );
            throw ExternalApiHttpFailures.fetchFailed("liveresultFetchFailed");
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://www.liveresult.ru";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
