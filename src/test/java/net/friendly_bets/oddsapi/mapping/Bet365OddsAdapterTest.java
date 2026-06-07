package net.friendly_bets.oddsapi.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.OddsMatchContext;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Bet365OddsAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Bet365OddsAdapter adapter = new Bet365OddsAdapter();

    @Test
    void mapsSpreadMinusOneToHomeAndAwayHandicaps() throws Exception {
        var resource = getClass().getResourceAsStream("/oddsapi/bet365/spread-minus-one.json");
        OddsApiMarketDto[] markets = objectMapper.readValue(resource, OddsApiMarketDto[].class);

        List<MappedOddsQuote> quotes = adapter.mapMarkets(
                List.of(markets[0]),
                OddsMatchContext.of("Home FC", "Away FC")
        );

        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_HOME_MINUS_1_0.getCode()));
        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getCode()));
        assertTrue(quotes.stream().noneMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode()));
    }

    @Test
    void mapsAlternativeAsianHandicapWithAwaySignInversion() throws Exception {
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

        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getSelectionCode().equals("HOME")
                && "-1.5".equals(q.getLine())
                && "5.500".equals(q.getOdds())));
        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getSelectionCode().equals("AWAY")
                && "1.5".equals(q.getLine())
                && "6.000".equals(q.getOdds())));
        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_HOME_PLUS_1_0.getCode()
                && "1.170".equals(q.getOdds())));
        assertTrue(quotes.stream().anyMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode()
                && "1.200".equals(q.getOdds())));
        assertTrue(quotes.stream().noneMatch(q -> q.isOk()
                && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getCode()));
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
