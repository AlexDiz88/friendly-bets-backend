package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreHttpClient.class);

    private final TwentyFourScoreProperties properties;
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
        return getHtml(url, base + "/football/");
    }

    private String getHtml(String url, String referer) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", referer)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
