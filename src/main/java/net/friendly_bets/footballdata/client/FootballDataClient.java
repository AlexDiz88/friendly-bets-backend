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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class FootballDataClient {

    private static final Logger log = LoggerFactory.getLogger(FootballDataClient.class);

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
        return fetchMatches(competitionCode, season, matchday, null);
    }

    public FootballDataMatchdayResponse fetchMatchesByStage(String competitionCode, String stage, String season) {
        return fetchMatches(competitionCode, season, null, stage);
    }

    private FootballDataMatchdayResponse fetchMatches(
            String competitionCode,
            String season,
            Integer matchday,
            String stage
    ) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/competitions/{code}/matches")
                .queryParam("season", season);

        if (stage != null && !stage.isBlank()) {
            uriBuilder.queryParam("stage", stage);
        } else if (matchday != null) {
            uriBuilder.queryParam("matchday", matchday);
        }

        String url = uriBuilder.buildAndExpand(competitionCode).toUriString();
        log.info(
                "football-data.org GET {}{} competition={} season={}",
                properties.getBaseUrl(),
                url,
                competitionCode,
                season
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", properties.getApiKey());

        try {
            ResponseEntity<FootballDataMatchdayResponse> response = footballDataRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    FootballDataMatchdayResponse.class
            );
            FootballDataMatchdayResponse body = response.getBody();
            int count = body != null && body.getMatches() != null ? body.getMatches().size() : 0;
            log.info("football-data.org response: {} matches for {}", count, url);
            return body;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("football-data.org rate limit for {}", url);
            throw new BadRequestException("footballDataRateLimitExceeded");
        } catch (HttpClientErrorException e) {
            log.warn("football-data.org HTTP {} for {}: {}", e.getStatusCode().value(), url, e.getMessage());
            throw new BadRequestException("footballDataRequestFailed");
        }
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }
}
