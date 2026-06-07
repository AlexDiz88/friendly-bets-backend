package net.friendly_bets.oddsapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsApiLeagueRefDto {

    private String name;
    private String slug;
}
