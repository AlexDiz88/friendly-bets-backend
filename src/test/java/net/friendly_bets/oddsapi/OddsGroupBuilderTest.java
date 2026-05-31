package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsGroupBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mergesDoubleChanceFromBothBookmakers() throws Exception {
        OddsApiMarketDto xbetDc = market("Double Chance", """
                [{"1X":"1.103","12":"1.250","X2":"2.740"}]
                """);
        OddsApiMarketDto bet365Dc = market("Double Chance", """
                [{"label":"Mexico or Draw","under":"1.111"},{"label":"Mexico or South Africa","under":"1.200"},{"label":"Draw or South Africa","under":"2.625"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(xbetDc));
        byBk.put("Bet365", List.of(bet365Dc));

        OddsMatchContext ctx = OddsMatchContext.of("Mexico", "South Africa");
        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet")),
                ctx
        );

        Optional<OddsMarketGroup> dc = groups.stream()
                .filter(g -> "DOUBLE_CHANCE".equals(g.getCategory()))
                .findFirst();
        assertTrue(dc.isPresent());
        assertEquals(3, dc.get().getRows().size());

        OddsLineRow dc1x = dc.get().getRows().stream()
                .filter(r -> "DC_1X".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("1.103", dc1x.getBookmakerOdds().get("1xbet"));
        assertEquals("1.111", dc1x.getBookmakerOdds().get("Bet365"));
    }

    @Test
    void mergesBttsDespiteSpuriousHandicapOnLabelRows() throws Exception {
        OddsApiMarketDto xbet = market("Both Teams To Score", """
                [{"yes":"2.190","no":"1.616"}]
                """);
        OddsApiMarketDto bet365 = market("Both Teams To Score", """
                [{"label":"Yes","hdp":0,"over":"2.200"},{"label":"No","hdp":0,"under":"1.615"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(xbet));
        byBk.put("Bet365", List.of(bet365));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet")),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        OddsMarketGroup btts = groups.stream()
                .filter(g -> "BTTS".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, btts.getRows().size());

        OddsLineRow yes = btts.getRows().stream()
                .filter(r -> "YES".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("2.190", yes.getBookmakerOdds().get("1xbet"));
        assertEquals("2.200", yes.getBookmakerOdds().get("Bet365"));
    }

    @Test
    void correctScoreKeepsEachScoreSeparate() throws Exception {
        OddsApiMarketDto bet365 = market("Correct Score", """
                [{"label":"1-0","over":"8.00"},{"label":"2-1","over":"12.00"},{"label":"0-0","over":"9.50"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("Bet365", List.of(bet365));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365")),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        OddsMarketGroup cs = groups.stream()
                .filter(g -> "CORRECT_SCORE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, cs.getRows().size());
        assertEquals("0-0", cs.getRows().get(0).getDisplayLabel());
        assertEquals("1-0", cs.getRows().get(1).getDisplayLabel());
        assertEquals("0-1", cs.getRows().get(2).getDisplayLabel());
    }

    @Test
    void mergesHandicapAcrossMarketNames() throws Exception {
        OddsApiMarketDto spread = market("Spread", """
                [{"hdp":-1,"home":"1.790","away":"2.030"}]
                """);
        OddsApiMarketDto asian = market("Alternative Asian Handicap", """
                [{"hdp":-1,"home":"1.800","away":"2.050"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(spread));
        byBk.put("Bet365", List.of(asian));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet")),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        Optional<OddsMarketGroup> handicap = groups.stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .findFirst();
        assertTrue(handicap.isPresent());
        assertEquals(2, handicap.get().getRows().size());
        assertTrue(handicap.get().getRows().stream().allMatch(r -> "-1".equals(r.getLine())));
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
