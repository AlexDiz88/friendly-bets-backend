package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class Soccer365HttpClient {

    private static final Logger log = LoggerFactory.getLogger(Soccer365HttpClient.class);

    private final Soccer365Properties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String fetchScheduleHtml(int competitionId) {
        String base = trimTrailingSlash(properties.getBaseUrl());
        String competitionUrl = base + "/competitions/" + competitionId + "/";
        String scheduleUrl = base + "/competitions/" + competitionId + "/shedule/";
        jitterSleep();
        return getHtml(scheduleUrl, competitionUrl);
    }

    private String getHtml(String url, String referer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .GET()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("User-Agent", Soccer365HttpHeaders.pickUserAgent())
                    .header("Referer", referer);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("soccer365 HTTP {} for {}", response.statusCode(), url);
                throw new BadRequestException("soccer365FetchFailed");
            }
            return response.body() != null ? response.body() : "";
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("soccer365 fetch failed for {}: {}", url, e.getMessage());
            throw new BadRequestException("soccer365FetchFailed");
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
            return "https://soccer365.ru";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
