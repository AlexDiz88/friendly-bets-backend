package net.friendly_bets.marathonbet.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Event odds from Marathonbet Pan API.
 * <p>
 * Prefer {@code Accept: application/json}: the same URL as SSE returns a finite JSON body
 * (~800KB). {@code Accept: text/event-stream} streams one huge {@code data:} line without a
 * terminating blank line for a long time — {@code BufferedReader.readLine()} hangs until the
 * entire line arrives (and can stall indefinitely from some egress IPs).
 */
@Component
@RequiredArgsConstructor
public class MarathonbetEventLineClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final ObjectMapper objectMapper;
    private final ExternalApiCircuitBreaker circuitBreaker;
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(CONNECT_TIMEOUT);

    public MarathonbetHttpFetchResult fetchEventSnapshot(long treeId) {
        long started = System.nanoTime();
        String url = MarathonbetPanHeaders.BASE_URL + "/eag/event-line/api/v1/events/" + treeId;
        // HTML pages are slug-based (/su/betting/event/...), but API still keys by treeId.
        // Numeric sport/event referer remains accepted by the API.
        String referer = MarathonbetPanHeaders.BASE_URL + "/su/betting/football";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(READ_TIMEOUT)
                .header("Accept", "application/json")
                .GET();
        MarathonbetPanHeaders.apply(builder, referer);

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            long durationMs = elapsedMs(started);
            Integer retryAfter = parseRetryAfter(response);
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                circuitBreaker.recordFailure(ExternalDataLayer.ODDS, "marathonbet", kind, "HTTP " + response.statusCode());
                return MarathonbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MarathonbetHttpOutcome.HTTP_ERROR)
                        .durationMs(durationMs)
                        .errorDetail("HTTP " + response.statusCode())
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
            String body = response.body() != null ? response.body() : "";
            if (body.isBlank()) {
                return MarathonbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MarathonbetHttpOutcome.PARSE_ERROR)
                        .durationMs(durationMs)
                        .errorDetail("empty event body")
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
            circuitBreaker.recordSuccess(ExternalDataLayer.ODDS);
            return MarathonbetHttpFetchResult.builder()
                    .success(true)
                    .httpStatus(response.statusCode())
                    .outcome(MarathonbetHttpOutcome.SUCCESS)
                    .durationMs(durationMs)
                    .body(objectMapper.readTree(body))
                    .retryAfterSeconds(retryAfter)
                    .build();
        } catch (HttpTimeoutException e) {
            circuitBreaker.recordFailure(ExternalDataLayer.ODDS, "marathonbet", ScrapeFailureKind.TIMEOUT, e.getMessage());
            return MarathonbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MarathonbetHttpOutcome.TIMEOUT)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        } catch (Exception e) {
            circuitBreaker.recordFailure(
                    ExternalDataLayer.ODDS,
                    "marathonbet",
                    ScrapeHttpSupport.classifyThrowable(e),
                    e.getMessage()
            );
            return MarathonbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MarathonbetHttpOutcome.NETWORK_ERROR)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        }
    }

    private static long elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000L;
    }

    private static Integer parseRetryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("retry-after")
                .flatMap(MarathonbetEventLineClient::parsePositiveInt)
                .or(() -> response.headers().firstValue("Retry-After")
                        .flatMap(MarathonbetEventLineClient::parsePositiveInt))
                .orElse(null);
    }

    private static Optional<Integer> parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
