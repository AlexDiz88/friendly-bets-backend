package net.friendly_bets.oddsapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsApiEventOddsDto {

    private Long id;
    private String home;
    private String away;
    private String date;
    private String status;
    /** Bookmaker name → list of markets. */
    private Map<String, java.util.List<OddsApiMarketDto>> bookmakers;
}
