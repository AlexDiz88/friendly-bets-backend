package net.friendly_bets.marathonbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MarathonbetEventLineClient {

    private static final String BASE_URL = "https://new.marathonbet.ru";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(180);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public JsonNode fetchEventSnapshot(long treeId) {
        String url = BASE_URL + "/eag/event-line/api/v1/events/" + treeId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(READ_TIMEOUT)
                .header("Accept", "text/event-stream")
                .header("Accept-Language", "ru-RU")
                .header("X-Pan-Source", "REDESIGN_WEB")
                .header("X-Pan-Version", "MOBILE-SSR-2.5.4")
                .header("X-Pan-Target", "BROWSER")
                .header("X-Country-Code", "RU")
                .header("Referer", BASE_URL + "/su/sport/event/" + treeId)
                .header("User-Agent", "FriendlyBets/1.0 (admin scrape)")
                .GET()
                .build();

        try {
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() >= 400) {
                throw new BadRequestException("marathonbetFetchFailed");
            }
            String payload = readSnapshotPayload(response.body());
            return objectMapper.readTree(payload);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("marathonbetFetchFailed");
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
        throw new BadRequestException("marathonbetParseFailed");
    }
}
