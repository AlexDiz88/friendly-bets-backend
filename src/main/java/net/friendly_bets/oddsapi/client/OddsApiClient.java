package net.friendly_bets.oddsapi.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OddsApiClient {

    private static final Logger log = LoggerFactory.getLogger(OddsApiClient.class);

    private final RestTemplate oddsApiRestTemplate;
    private final OddsApiProperties properties;

    public List<OddsApiEventDto> fetchEvents(String leagueSlug, String status) {
        requireConfigured();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/events")
                .queryParam("apiKey", properties.getApiKey())
                .queryParam("sport", "football")
                .queryParam("league", leagueSlug);

        if (status != null && !status.isBlank()) {
            builder.queryParam("status", status);
        }

        String url = builder.toUriString();
        log.info("odds-api.io GET events league={} status={}", leagueSlug, status);

        try {
            ResponseEntity<List<OddsApiEventDto>> response = oddsApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<OddsApiEventDto> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("odds-api.io rate limit for {}", url);
            throw new BadRequestException("oddsApiRateLimitExceeded");
        } catch (HttpClientErrorException e) {
            log.warn("odds-api.io HTTP {} for {}: {}", e.getStatusCode().value(), url, e.getMessage());
            throw new BadRequestException("oddsApiRequestFailed");
        }
    }

    public List<OddsApiEventOddsDto> fetchOddsMulti(List<Long> eventIds, List<String> bookmakers) {
        requireConfigured();
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (eventIds.size() > 10) {
            throw new IllegalArgumentException("odds-api multi supports at most 10 event ids");
        }

        String ids = eventIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        String bookmakerParam = String.join(",", bookmakers);

        String url = UriComponentsBuilder
                .fromPath("/odds/multi")
                .queryParam("apiKey", properties.getApiKey())
                .queryParam("eventIds", ids)
                .queryParam("bookmakers", bookmakerParam)
                .toUriString();

        log.info("odds-api.io GET odds/multi events={} bookmakers={}", ids, bookmakerParam);

        try {
            ResponseEntity<List<OddsApiEventOddsDto>> response = oddsApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<OddsApiEventOddsDto> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("odds-api.io rate limit for {}", url);
            throw new BadRequestException("oddsApiRateLimitExceeded");
        } catch (HttpClientErrorException e) {
            log.warn("odds-api.io HTTP {} for {}: {}", e.getStatusCode().value(), url, e.getMessage());
            throw new BadRequestException("oddsApiRequestFailed");
        }
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BadRequestException("oddsApiKeyNotConfigured");
        }
    }
}
