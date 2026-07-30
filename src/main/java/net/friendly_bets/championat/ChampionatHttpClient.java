package net.friendly_bets.championat;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.championat.config.ChampionatProperties;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
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
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ChampionatHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ChampionatHttpClient.class);

    private final ChampionatProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));

    /**
     * Match-center JSON for a UTC calendar date: {@code /stat/data/{yyyy-MM-dd}/football}.
     */
    public String fetchDateFootballJson(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("championatFetchFailed");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String url = base + "/stat/data/" + date + "/football";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getBody(url, base + "/stat/football/", true);
    }

    public String fetchTournamentTableHtml(String tournamentPath) {
        if (tournamentPath == null || tournamentPath.isBlank()) {
            throw new BadRequestException("championatTournamentNotConfigured");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String path = tournamentPath.startsWith("/") ? tournamentPath : "/" + tournamentPath;
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String url = base + path + "table/";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getBody(url, base + path, false);
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
            if (ScrapeHttpSupport.looksLikeJsChallenge(body)) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.LIVE, ExternalProviderIds.CHAMPIONAT, ScrapeFailureKind.CHALLENGE, url);
                throw new BadRequestException("championatFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("championat HTTP {} for {}", response.statusCode(), url);
                circuitBreaker.recordFailure(
                        ExternalDataLayer.LIVE, ExternalProviderIds.CHAMPIONAT, kind, "HTTP " + response.statusCode());
                throw new BadRequestException("championatFetchFailed");
            }
            circuitBreaker.recordSuccess(ExternalDataLayer.LIVE);
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("championat fetch failed for {}: {}", url, e.getMessage());
            circuitBreaker.recordFailure(
                    ExternalDataLayer.LIVE, ExternalProviderIds.CHAMPIONAT, kind, e.getMessage());
            throw new BadRequestException("championatFetchFailed");
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.championat.com";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
