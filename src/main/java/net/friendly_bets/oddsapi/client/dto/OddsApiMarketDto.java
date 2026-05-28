package net.friendly_bets.oddsapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsApiMarketDto {

    private String name;
    private String updatedAt;
    /** Outcome rows — shape varies by market (ML, Totals, …). */
    private List<JsonNode> odds;
}
