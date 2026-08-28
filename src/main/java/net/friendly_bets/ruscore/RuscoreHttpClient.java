package net.friendly_bets.ruscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.ruscore.config.RuscoreProperties;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bot-prerender HTML client for ruscore.ru (Googlebot UA → full {@code __NUXT_DATA__}).
 */
@Component
@RequiredArgsConstructor
public class RuscoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(RuscoreHttpClient.class);
    private static final String BOT_UA = "Googlebot/2.1 (+http://www.google.com/bot.html)";

    private final RuscoreProperties properties;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(30));
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);

    public String fetchDayFootballHtml(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("ruscoreFetchFailed");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String url = base + "/football/" + date;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/football/", ExternalDataLayer.FULL_MATCH);
    }

    public String fetchGameSummaryHtml(String slug, String eventId) {
        if (slug == null || slug.isBlank() || eventId == null || eventId.isBlank()) {
            throw new BadRequestException("ruscoreGameIdRequired");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String path = "/game/" + slug.trim() + "/" + eventId.trim() + "/summary";
        String url = base + path;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/game/" + slug.trim() + "/" + eventId.trim(), ExternalDataLayer.FULL_MATCH);
    }

    public String fetchTournamentCalendarHtml(String tournamentSlug, int seasonId) {
        if (tournamentSlug == null || tournamentSlug.isBlank() || seasonId <= 0) {
            throw new BadRequestException("ruscoreTournamentNotConfigured");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String url = base + "/tournament/" + tournamentSlug.trim() + "/" + seasonId + "/calendar";
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/tournament/" + tournamentSlug.trim() + "/" + seasonId, ExternalDataLayer.FULL_MATCH);
    }

    private void warmHomepage(String base) {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
            getHtmlInternal(base + "/", null, ExternalDataLayer.FULL_MATCH, false);
        } catch (RuntimeException e) {
            log.info("ruscore homepage warm-up failed: {}", e.getMessage());
        }
    }

    private String getHtml(String url, String referer, ExternalDataLayer layer) {
        return getHtmlInternal(url, referer, layer, true);
    }

    private String getHtmlInternal(String url, String referer, ExternalDataLayer layer, boolean reportCircuit) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            // Override UA: browser profile yields empty Nuxt shell; bot prerender has full data.
            builder.setHeader("User-Agent", BOT_UA);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeAccessWall(body)) {
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.RUSCORE, ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("ruscoreFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("ruscore HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.RUSCORE, kind, "HTTP " + response.statusCode());
                }
                throw ExternalApiHttpFailures.fetchFailed("ruscoreFetchFailed");
            }
            if (body.length() < 20_000 && body.contains("data-ssr=\"false\"")) {
                log.warn("ruscore returned Nuxt shell without prerender for {}", url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(layer, ExternalProviderIds.RUSCORE, ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("ruscoreFetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(layer);
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("ruscore fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(layer, ExternalProviderIds.RUSCORE, kind, e.getMessage());
            }
            throw ExternalApiHttpFailures.fetchFailed("ruscoreFetchFailed");
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://ruscore.ru";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
