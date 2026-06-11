package net.friendly_bets.marathonbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarathonbetEventLineClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(180);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public MarathonbetHttpFetchResult fetchEventSnapshot(long treeId) {
        long started = System.nanoTime();
        String url = MarathonbetPanHeaders.BASE_URL + "/eag/event-line/api/v1/events/" + treeId;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(READ_TIMEOUT)
                .header("Accept", "text/event-stream")
                .GET();
        MarathonbetPanHeaders.apply(builder, MarathonbetPanHeaders.BASE_URL + "/su/sport/event/" + treeId);

        try {
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            long durationMs = elapsedMs(started);
            Integer retryAfter = parseRetryAfter(response);
            if (response.statusCode() >= 400) {
                return MarathonbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MarathonbetHttpOutcome.HTTP_ERROR)
                        .durationMs(durationMs)
                        .errorDetail("HTTP " + response.statusCode())
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
            String payload = readSnapshotPayload(response.body());
            return MarathonbetHttpFetchResult.builder()
                    .success(true)
                    .httpStatus(response.statusCode())
                    .outcome(MarathonbetHttpOutcome.SUCCESS)
                    .durationMs(durationMs)
                    .body(objectMapper.readTree(payload))
                    .retryAfterSeconds(retryAfter)
                    .build();
        } catch (HttpTimeoutException e) {
            return MarathonbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MarathonbetHttpOutcome.TIMEOUT)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        } catch (MarathonbetParseException e) {
            return MarathonbetHttpFetchResult.builder()
                    .success(false)
                    .httpStatus(e.getHttpStatus())
                    .outcome(MarathonbetHttpOutcome.PARSE_ERROR)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        } catch (Exception e) {
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

    private static String readSnapshotPayload(java.io.InputStream body) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            StringBuilder buffer = new StringBuilder();
            boolean snapshotEvent = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    snapshotEvent = line.contains("snapshot");
                    buffer.setLength(0);
                    continue;
                }
                if (!snapshotEvent) {
                    continue;
                }
                if (line.startsWith("data:")) {
                    buffer.append(line.substring(5).trim());
                    continue;
                }
                if (line.isEmpty() && buffer.length() > 0) {
                    return buffer.toString();
                }
            }
            if (buffer.length() > 0) {
                return buffer.toString();
            }
        }
        throw new MarathonbetParseException("snapshot not found in SSE stream", null);
    }

    private static final class MarathonbetParseException extends Exception {
        private final Integer httpStatus;

        private MarathonbetParseException(String message, Integer httpStatus) {
            super(message);
            this.httpStatus = httpStatus;
        }

        private Integer getHttpStatus() {
            return httpStatus;
        }
    }
}
