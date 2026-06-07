package net.friendly_bets.api_football.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballFixturesResponse {

    private List<ApiFootballFixtureDto> response;
}
