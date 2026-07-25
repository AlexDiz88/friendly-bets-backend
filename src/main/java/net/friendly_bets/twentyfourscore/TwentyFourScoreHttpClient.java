package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreHttpClient.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final TwentyFourScoreProperties properties;
    private final TwentyFourScoreStandingsParser standingsParser;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String fetchDateFootballHtml(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }
        String base = trimTrailingSlash(properties.getBaseUrl());
        String url = base + "/football/?date=" + date;
        jitterSleep();
        return getHtml(httpClient, url, base + "/football/");
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

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpClient cookieClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookieManager)
                .build();

        jitterSleep();
        String shellHtml = getHtml(cookieClient, standingsUrl, base + "/");
        String dataKey = standingsParser.extractDataKey(shellHtml);
        if (dataKey == null) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }

        String dataUrl = base + "/backend/load_page_data.php?data_key="
                + URLEncoder.encode(dataKey, StandardCharsets.UTF_8);
        jitterSleep();
        String dataHtml = getHtml(cookieClient, dataUrl, standingsUrl);
        if (dataHtml == null || dataHtml.isBlank() || dataHtml.contains("data error")) {
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }
        return dataHtml;
    }

    private String getHtml(HttpClient client, String url, String referer) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", referer)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("24score HTTP {} for {}", response.statusCode(), url);
                throw new BadRequestException("twentyFourScoreFetchFailed");
            }
            return response.body() != null ? response.body() : "";
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("24score fetch failed for {}: {}", url, e.getMessage());
            throw new BadRequestException("twentyFourScoreFetchFailed");
        }
    }

    void jitterSleep() {
        long min = Math.max(0L, properties.getHttpDelayMinMs());
        long max = Math.max(min, properties.getHttpDelayMaxMs());
        long delay = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
