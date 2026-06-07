package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.odds.OddsMarket;
import net.friendly_bets.models.odds.OddsOutcome;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsApiMarketMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseMlMarket() throws Exception {
        OddsApiMarketDto dto = new OddsApiMarketDto();
        dto.setName("ML");
        dto.setOdds(List.of(objectMapper.readTree("""
                {"home":"2.10","draw":"3.40","away":"3.50"}
                """)));

        List<OddsMarket> markets = OddsApiMarketMapper.toMarkets(List.of(dto));
        assertEquals(1, markets.size());
        assertEquals("ML", markets.get(0).getName());
        assertEquals(3, markets.get(0).getOutcomes().size());
        assertTrue(markets.get(0).getOutcomes().stream()
                .anyMatch(o -> "home".equals(o.getLabel()) && "2.10".equals(o.getOdds())));
    }

    @Test
    void parseTotalsWithLine() throws Exception {
        OddsApiMarketDto dto = new OddsApiMarketDto();
        dto.setName("Totals");
        dto.setOdds(List.of(objectMapper.readTree("""
                {"hdp":"2.5","over":"1.90","under":"1.95"}
                """)));

        List<OddsOutcome> outcomes = OddsApiMarketMapper.parseOutcomes(dto.getOdds());
        assertEquals(2, outcomes.size());
        assertTrue(outcomes.stream().allMatch(o -> "2.5".equals(o.getLine())));
    }
}
