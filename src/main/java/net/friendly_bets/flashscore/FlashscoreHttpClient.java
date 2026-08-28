package net.friendly_bets.flashscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscore.config.FlashscoreProperties;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class FlashscoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(FlashscoreHttpClient.class);

    private final FlashscoreProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(30));

    public String fetchDayFootballFeed(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("flashscoreFetchFailed");
        }
        long dayOffset = feedDayOffset(date, LocalDate.now(properties.feedZone()));
        String path = String.format("/2/x/feed/f_1_%d_3_%s_1", dayOffset, properties.getFeedLocale());
        return fetchFeed(path, ExternalDataLayer.FULL_MATCH);
    }

    /**
     * Flashscore {@code f_1_{offset}} is relative to the edition calendar today, not UTC.
     */
    static long feedDayOffset(LocalDate requested, LocalDate feedToday) {
        if (requested == null || feedToday == null) {
            throw new BadRequestException("flashscoreFetchFailed");
        }
        return ChronoUnit.DAYS.between(feedToday, requested);
    }

    public String fetchMatchSummaryFeed(String eventId) {
        return fetchMatchDetailFeed("df_sui_1_" + requireEventId(eventId));
    }

    public String fetchMatchStatsFeed(String eventId) {
        return fetchMatchDetailFeed("df_st_1_" + requireEventId(eventId));
    }

    public String fetchMatchResultFeed(String eventId) {
        return fetchMatchDetailFeed("df_sur_1_" + requireEventId(eventId));
    }

    public String fetchMatchH2HFeed(String eventId) {
        return fetchMatchDetailFeed("df_hh_1_" + requireEventId(eventId));
    }

    public String fetchTournamentPageHtml(String tournamentPath) {
        return fetchTournamentPageHtml(tournamentPath, properties.getBaseUrl());
    }

    public String fetchTournamentPageHtml(String tournamentPath, String baseUrl) {
        if (tournamentPath == null || tournamentPath.isBlank()) {
            throw new BadRequestException("flashscoreTournamentNotConfigured");
        }
        String base = trimTrailingSlash(baseUrl != null && !baseUrl.isBlank() ? baseUrl : properties.getBaseUrl());
        String path = tournamentPath.startsWith("/") ? tournamentPath : "/" + tournamentPath;
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getText(url, base + "/football/", ExternalDataLayer.FULL_MATCH, true);
    }

    private String fetchMatchDetailFeed(String feedName) {
        String path = "/2/x/feed/" + feedName;
        return fetchFeed(path, ExternalDataLayer.FULL_MATCH);
    }

    private String fetchFeed(String path, ExternalDataLayer layer) {
        String base = trimTrailingSlash(properties.getFeedBaseUrl());
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getText(url, properties.getBaseUrl() + "/football/", layer, true);
    }

    private String getText(String url, String referer, ExternalDataLayer layer, boolean reportCircuit) {
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
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.FLASHSCORE, ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("flashscoreFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("flashscore HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.FLASHSCORE, kind, "HTTP " + response.statusCode());
                }
                throw ExternalApiHttpFailures.fetchFailed("flashscoreFetchFailed");
            }
            if (body.length() < 5 && !"0".equals(body.trim())) {
                log.warn("flashscore empty feed for {}", url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.FLASHSCORE, ScrapeFailureKind.PARSE_ERROR, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("flashscoreFetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(layer);
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("flashscore fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(layer, ExternalProviderIds.FLASHSCORE, kind, e.getMessage());
            }
            throw ExternalApiHttpFailures.fetchFailed("flashscoreFetchFailed");
        }
    }

    private static String requireEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("flashscoreGameIdRequired");
        }
        return eventId.trim();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.flashscorekz.com";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
