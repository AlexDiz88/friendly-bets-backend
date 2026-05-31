package net.friendly_bets.api_football.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.api_football.client.dto.ApiFootballFixtureDto;
import net.friendly_bets.api_football.client.dto.ApiFootballFixturesResponse;
import net.friendly_bets.api_football.config.ApiFootballProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiFootballClient {

    private final RestTemplate apiFootballRestTemplate;
    private final ApiFootballProperties properties;

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public List<ApiFootballFixtureDto> fetchFixturesByDate(int leagueId, int season, LocalDate date) {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder
                .fromPath("/fixtures")
                .queryParam("league", leagueId)
                .queryParam("season", season)
                .queryParam("date", date)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApiKey());

        ResponseEntity<ApiFootballFixturesResponse> response = apiFootballRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiFootballFixturesResponse.class
        );
        ApiFootballFixturesResponse body = response.getBody();
        if (body == null || body.getResponse() == null) {
            return Collections.emptyList();
        }
        return body.getResponse();
    }
}
