package net.friendly_bets.fifa.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.fifa.config.FifaProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FifaHttpClient {

    private static final int PAGE_SIZE = 500;

    private final RestTemplate fifaRestTemplate;
    private final FifaProperties properties;
    private final ObjectMapper objectMapper;

    public List<JsonNode> fetchAllCalendarMatches() {
        List<JsonNode> all = new ArrayList<>();
        String continuationToken = null;
        do {
            String url = buildCalendarUrl(continuationToken);
            JsonNode body = fetchJson(url);
            if (body == null) {
                break;
            }
            JsonNode results = body.get("Results");
            if (results != null && results.isArray()) {
                results.forEach(all::add);
            }
            JsonNode tokenNode = body.get("ContinuationToken");
            continuationToken = tokenNode != null && !tokenNode.isNull() ? tokenNode.asText(null) : null;
        } while (continuationToken != null && !continuationToken.isBlank());
        return all;
    }

    /** Официальные позиции и статистика группового этапа (как на fifa.com/standings). */
    public JsonNode fetchGroupStageStandings() {
        String url = properties.getBaseUrl().replaceAll("/$", "")
                + "/api/v3/calendar/"
                + properties.getCompetitionId()
                + "/"
                + properties.getSeasonId()
                + "/"
                + properties.getStageId()
                + "/Standing?language=en";
        return fetchJson(url);
    }

    private String buildCalendarUrl(String continuationToken) {
        String base = properties.getBaseUrl().replaceAll("/$", "")
                + "/api/v3/calendar/matches?count=" + PAGE_SIZE
                + "&idSeason=" + properties.getSeasonId()
                + "&idCompetition=" + properties.getCompetitionId()
                + "&language=en";
        if (continuationToken != null && !continuationToken.isBlank()) {
            return base + "&continuationToken=" + continuationToken;
        }
        return base;
    }

    private JsonNode fetchJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());
        headers.set(HttpHeaders.ACCEPT, "application/json");
        ResponseEntity<String> response = fifaRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
