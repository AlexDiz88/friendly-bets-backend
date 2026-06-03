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
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MarathonbetTournamentClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public JsonNode fetchTournament(long tournamentTreeId) {
        if (tournamentTreeId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
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
            if (response.statusCode() >= 400) {
                throw new BadRequestException("marathonbetFetchFailed");
            }
            return objectMapper.readTree(response.body());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("marathonbetFetchFailed");
        }
    }
}
