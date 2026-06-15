package net.friendly_bets.twentyfourscore.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreHttpClient {

    private static final DateTimeFormatter DATE_PARAM = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate twentyFourScoreRestTemplate;
    private final TwentyFourScoreProperties properties;
    private final AtomicLong lastRequestAtMs = new AtomicLong(0L);

    public String fetchDailyPage(LocalDate date) {
        String path = "/football/?date=" + DATE_PARAM.format(date);
        return fetchHtml(properties.getBaseUrl().replaceAll("/$", "") + path);
    }

    public String fetchMatchPage(String matchPath) {
        return fetchHtml(resolveUrl(matchPath));
    }

    public String fetchCompetitionPath(String competitionPath) {
        return fetchHtml(resolveUrl(competitionPath));
    }

    private String resolveUrl(String path) {
        if (path == null || path.isBlank()) {
            return properties.getBaseUrl().replaceAll("/$", "") + "/";
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return properties.getBaseUrl().replaceAll("/$", "") + normalized;
    }

    private String fetchHtml(String url) {
        throttle(properties.getRequestDelayMs());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9");
        ResponseEntity<String> response = twentyFourScoreRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        return response.getBody() != null ? response.getBody() : "";
    }

    private void throttle(long delayMs) {
        long delay = Math.max(0L, delayMs);
        if (delay == 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastRequestAtMs.get();
        long wait = delay - (now - last);
        if (wait > 0L) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAtMs.set(System.currentTimeMillis());
    }
}
