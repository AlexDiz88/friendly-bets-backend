package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreHttpClient.class);

    private final TwentyFourScoreProperties properties;
    private final TwentyFourScoreStandingsParser standingsParser;
    private final ExternalApiCircuitBreaker circuitBreaker;

    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(20));
    private final AtomicBoolean homepageWarmed = new AtomicBoolean(false);

    public String fetchDateFootballHtml(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        warmHomepage(base);
        String url = base + "/football/?date=" + date;
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        return getHtml(url, base + "/football/", false);
    }

    /**
     * Standings team list is loaded via cookie-bound AJAX ({@code load_page_data.php}).
     * One shell GET + one data GET; cookies must be kept for the pair.
     */
    public String fetchStandingsDataHtml(String standingsPath) {
        if (standingsPath == null || standingsPath.isBlank()) {
            throw new BadRequestException("twentyFourScoreStandingsNotConfigured");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String path = standingsPath.startsWith("/") ? standingsPath : "/" + standingsPath;
        String standingsUrl = base + path;

        warmHomepage(base);
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        String shellHtml = getHtml(standingsUrl, base + "/", false);
        String dataKey = standingsParser.extractDataKey(shellHtml);
        if (dataKey == null) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }

        String dataUrl = base + "/backend/load_page_data.php?data_key="
                + URLEncoder.encode(dataKey, StandardCharsets.UTF_8);
        ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
        String dataHtml = getHtml(dataUrl, standingsUrl, true);
        if (dataHtml == null || dataHtml.isBlank() || dataHtml.contains("data error")) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }
        return dataHtml;
    }

    private void warmHomepage(String base) {
        if (!homepageWarmed.compareAndSet(false, true)) {
            return;
        }
        try {
            ScrapeHttpSupport.jitterSleep(properties.getHttpDelayMinMs(), properties.getHttpDelayMaxMs());
            getHtmlInternal(base + "/", null, false, false);
        } catch (RuntimeException e) {
            log.info("24score homepage warm-up failed: {}", e.getMessage());
        }
    }

    private String getHtml(String url, String referer, boolean xhr) {
        return getHtmlInternal(url, referer, xhr, true);
    }

    private String getHtmlInternal(String url, String referer, boolean xhr, boolean reportCircuit) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET();
            if (xhr) {
                ScrapeHttpSupport.applyXhrHeaders(builder, browserProfile, referer);
            } else {
                ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, referer);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (ScrapeHttpSupport.looksLikeAccessWall(body)) {
                if (reportCircuit) {
                    circuitBreaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.CHALLENGE, url);
                }
                throw ExternalApiHttpFailures.fetchFailed("twentyFourScoreFetchFailed");
            }
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                log.warn("24score HTTP {} for {}", response.statusCode(), url);
                if (reportCircuit) {
                    circuitBreaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", kind, "HTTP " + response.statusCode());
                }
                throw ExternalApiHttpFailures.fetchFailed("twentyFourScoreFetchFailed");
            }
            if (reportCircuit) {
                circuitBreaker.recordSuccess(ExternalDataLayer.LIVE);
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            ScrapeFailureKind kind = ScrapeHttpSupport.classifyThrowable(e);
            log.warn("24score fetch failed for {}: {}", url, e.getMessage());
            if (reportCircuit) {
                circuitBreaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", kind, e.getMessage());
            }
            throw ExternalApiHttpFailures.fetchFailed("twentyFourScoreFetchFailed");
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://24score.pro";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
