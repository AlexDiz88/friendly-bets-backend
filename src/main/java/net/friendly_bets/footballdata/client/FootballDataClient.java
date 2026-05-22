package net.friendly_bets.footballdata.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.client.dto.FootballDataCompetitionResponse;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchdayResponse;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class FootballDataClient {

    private final RestTemplate footballDataRestTemplate;
    private final FootballDataProperties properties;

    public FootballDataCompetitionResponse fetchCompetition(String competitionCode) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }

        String url = UriComponentsBuilder
                .fromPath("/competitions/{code}")
                .buildAndExpand(competitionCode)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", properties.getApiKey());

        try {
            ResponseEntity<FootballDataCompetitionResponse> response = footballDataRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    FootballDataCompetitionResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BadRequestException("footballDataRateLimitExceeded");
        } catch (HttpClientErrorException e) {
            throw new BadRequestException("footballDataRequestFailed");
        }
    }

    public FootballDataMatchdayResponse fetchMatchday(String competitionCode, int matchday, String season) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }

        String url = UriComponentsBuilder
                .fromPath("/competitions/{code}/matches")
                .queryParam("matchday", matchday)
                .queryParam("season", season)
                .buildAndExpand(competitionCode)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", properties.getApiKey());

        try {
            ResponseEntity<FootballDataMatchdayResponse> response = footballDataRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    FootballDataMatchdayResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BadRequestException("footballDataRateLimitExceeded");
        } catch (HttpClientErrorException e) {
            throw new BadRequestException("footballDataRequestFailed");
        }
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }
}
