package net.friendly_bets.oddsapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsApiEventDto {

    private Long id;
    private String home;
    private Integer homeId;
    private String away;
    private Integer awayId;
    private String date;
    private String status;
    private OddsApiLeagueRefDto league;
}
