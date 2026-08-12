package net.friendly_bets.football24;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.football24.config.Football24Properties;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class Football24HttpClient {

    private static final Logger log = LoggerFactory.getLogger(Football24HttpClient.class);

    private final Football24Properties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);

    public String fetchSeasonsJson(int leagueId) {
        warmHomepage();
        String url = trimTrailingSlash(properties.getApiBaseUrl())
                + "/season/getSeasonsByLeagueId?leagueId=" + leagueId;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getJson(url, siteReferer(null));
    }

    /**
     * One-shot full calendar for a season (all rounds + {@code startingAt} UTC).
     * No per-match page requests.
     */
    public String fetchFixturesRoundsJson(int seasonId) {
        warmHomepage();
        String url = trimTrailingSlash(properties.getApiBaseUrl())
                + "/fixture/getFixturesRounds?seasonId=" + seasonId;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getJson(url, siteReferer(null));
    }

    public String fetchFixturesRoundsJson(int seasonId, String tournamentSlug) {
        warmHomepage();
        String url = trimTrailingSlash(properties.getApiBaseUrl())
                + "/fixture/getFixturesRounds?seasonId=" + seasonId;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getJson(url, siteReferer(tournamentSlug));
    }

    private void warmHomepage() {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            String site = trimTrailingSlash(properties.getSiteBaseUrl());
            ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
            getJsonInternal(site + "/ru/", null, false);
        } catch (RuntimeException e) {
            log.info("football24 homepage warm-up failed: {}", e.getMessage());
        }
    }

    private String siteReferer(String tournamentSlug) {
        String site = trimTrailingSlash(properties.getSiteBaseUrl());
        if (tournamentSlug != null && !tournamentSlug.isBlank()) {
            return site + "/ru/tournament/" + tournamentSlug.trim() + "/calendar";
        }
        return site + "/ru/";
    }

    private String getJson(String url, String referer) {
        return getJsonInternal(url, referer, true);
    }

    private String getJsonInternal(String url, String referer, boolean reportCircuit) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .header("Accept", "application/json")
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeJsChallenge(body)) {
                if (reportCircuit) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.SCHEDULE,
                            ExternalProviderIds.FOOTBALL24,
                            ScrapeFailureKind.CHALLENGE,
                            url
                    );
                }
                throw ExternalApiHttpFailures.fetchFailed("football24FetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("football24 HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.SCHEDULE,
                            ExternalProviderIds.FOOTBALL24,
                            kind,
                            "HTTP " + response.statusCode()
                    );
                }
                throw ExternalApiHttpFailures.fetchFailed("football24FetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(ExternalDataLayer.SCHEDULE);
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("football24 fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.SCHEDULE,
                        ExternalProviderIds.FOOTBALL24,
                        kind,
                        e.getMessage()
                );
            }
            throw ExternalApiHttpFailures.fetchFailed("football24FetchFailed");
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.football24.ua";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
