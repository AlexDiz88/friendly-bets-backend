package net.friendly_bets.marathonbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import org.springframework.stereotype.Component;

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
public class MarathonbetTournamentClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public MarathonbetHttpFetchResult fetchTournament(long tournamentTreeId) {
        if (tournamentTreeId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
        long started = System.nanoTime();
        String url = MarathonbetPanHeaders.BASE_URL + "/eag/event-line/api/v1/tournaments/" + tournamentTreeId;
        String referer = MarathonbetPanHeaders.BASE_URL + "/su/sport/tournaments/" + tournamentTreeId;
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
                return MarathonbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MarathonbetHttpOutcome.HTTP_ERROR)
                        .durationMs(durationMs)
                        .errorDetail(truncateBody(response.body()))
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
            return MarathonbetHttpFetchResult.builder()
                    .success(true)
                    .httpStatus(response.statusCode())
                    .outcome(MarathonbetHttpOutcome.SUCCESS)
                    .durationMs(durationMs)
                    .body(objectMapper.readTree(response.body()))
                    .retryAfterSeconds(retryAfter)
                    .build();
        } catch (HttpTimeoutException e) {
            return MarathonbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MarathonbetHttpOutcome.TIMEOUT)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        } catch (BadRequestException e) {
            throw e;
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
        return Optional.ofNullable(response.headers().firstValue("retry-after"))
                .flatMap(MarathonbetTournamentClient::parsePositiveInt)
                .or(() -> Optional.ofNullable(response.headers().firstValue("Retry-After"))
                        .flatMap(MarathonbetTournamentClient::parsePositiveInt))
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

    private static String truncateBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        return body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }
}
