package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.odds.MergedOddsLine;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsLineMergerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mergeCombinesTwoBookmakersOnMl() throws Exception {
        OddsApiMarketDto bet365Ml = market("ML", """
                [{"home":"1.480","draw":"4.333","away":"6.500"}]
                """);
        OddsApiMarketDto xbetMl = market("ML", """
                [{"home":"1.500","draw":"4.330","away":"7.700"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("Bet365", List.of(bet365Ml));
        byBk.put("1xbet", List.of(xbetMl));

        Map<String, String> canonical = OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet"));
        List<MergedOddsLine> merged = OddsLineMerger.merge(byBk, canonical);

        assertEquals(1, merged.size());
        MergedOddsLine ml = merged.get(0);
        assertEquals("ML", ml.getMarketName());
        assertTrue(ml.getSelections().stream().anyMatch(s ->
                "1.480".equals(s.getBookmakerOdds().get("Bet365"))
                        && "1.500".equals(s.getBookmakerOdds().get("1xbet"))
                        && "home".equals(s.getSelectionKey())
        ));
    }

    @Test
    void mergeFiltersQuarterTotals() throws Exception {
        OddsApiMarketDto totals = market("Totals", """
                [{"hdp":2.5,"over":"2.050","under":"1.800"},{"hdp":2.25,"over":"1.818","under":"2.012"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = Map.of("Bet365", List.of(totals));
        Map<String, String> canonical = OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365"));

        List<MergedOddsLine> merged = OddsLineMerger.merge(byBk, canonical);
        assertEquals(1, merged.size());
        assertEquals("2.5", merged.get(0).getLine());
    }

    private OddsApiMarketDto market(String name, String oddsJson) throws Exception {
        OddsApiMarketDto dto = new OddsApiMarketDto();
        dto.setName(name);
        var root = objectMapper.readTree(oddsJson);
        List<com.fasterxml.jackson.databind.JsonNode> rows = new java.util.ArrayList<>();
        root.forEach(rows::add);
        dto.setOdds(rows);
        return dto;
    }
}
