package net.friendly_bets.fourscore.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.fourscore.config.FourScoreProperties;
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
public class FourScoreHttpClient {

    private static final DateTimeFormatter DATE_PARAM = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate fourScoreRestTemplate;
    private final FourScoreProperties properties;
    private final AtomicLong lastRequestAtMs = new AtomicLong(0L);

    public String fetchEventsPage(LocalDate date) {
        String path = "/events/?date=" + DATE_PARAM.format(date);
        return fetchHtml(properties.getBaseUrl().replaceAll("/$", "") + path, properties.getRequestDelayMs());
    }

    public String fetchEventPage(String eventPath) {
        return fetchHtml(resolveEventUrl(eventPath), properties.getRequestDelayMs());
    }

    public String fetchEventPageForPreview(String eventPath) {
        return fetchHtml(resolveEventUrl(eventPath), properties.getPreviewRequestDelayMs());
    }

    private String resolveEventUrl(String eventPath) {
        if (eventPath == null || eventPath.isBlank()) {
            return properties.getBaseUrl().replaceAll("/$", "") + "/";
        }
        String trimmed = eventPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.endsWith("/") ? trimmed : trimmed + "/";
        }
        String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return properties.getBaseUrl().replaceAll("/$", "") + path;
    }

    private String fetchHtml(String url, long delayMs) {
        throttle(delayMs);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9");
        ResponseEntity<String> response = fourScoreRestTemplate.exchange(
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
