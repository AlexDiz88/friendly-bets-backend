package net.friendly_bets.oddsapi.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.oddsapi.OddsMatchContext;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Bet365OddsAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Bet365OddsAdapter adapter = new Bet365OddsAdapter();

    @Test
    void skipsSpreadHandicapMarket() throws Exception {
        var resource = getClass().getResourceAsStream("/oddsapi/bet365/spread-minus-one.json");
        OddsApiMarketDto[] markets = objectMapper.readValue(resource, OddsApiMarketDto[].class);

        List<MappedOddsQuote> quotes = adapter.mapMarkets(
                List.of(markets[0]),
                OddsMatchContext.of("Home FC", "Away FC")
        );

        assertTrue(quotes.isEmpty());
    }

    @Test
    void skipsAlternativeAsianHandicapMarket() throws Exception {
        OddsApiMarketDto asian = market("Alternative Asian Handicap", """
                [
                  {"hdp":-1.5,"home":"5.500","away":"6.000"},
                  {"hdp":1,"home":"1.170","away":"1.200"}
                ]
                """);

        List<MappedOddsQuote> quotes = adapter.mapMarkets(
                List.of(asian),
                OddsMatchContext.of("South Korea", "Czechia")
        );

        assertTrue(quotes.isEmpty());
    }

    @Test
    void skipsDoubleChanceMarket() throws Exception {
        OddsApiMarketDto dc = market("Double Chance", """
                [{"label":"Mexico or Draw","under":"1.111"}]
                """);

        List<MappedOddsQuote> quotes = adapter.mapMarkets(
                List.of(dc),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        assertTrue(quotes.isEmpty());
    }

    private OddsApiMarketDto market(String name, String oddsJson) throws Exception {
        OddsApiMarketDto dto = new OddsApiMarketDto();
        dto.setName(name);
        var root = objectMapper.readTree(oddsJson);
        java.util.List<com.fasterxml.jackson.databind.JsonNode> rows = new java.util.ArrayList<>();
        root.forEach(rows::add);
        dto.setOdds(rows);
        return dto;
    }

}
