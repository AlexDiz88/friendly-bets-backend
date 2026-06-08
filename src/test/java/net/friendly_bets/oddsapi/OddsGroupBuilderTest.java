package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.enums.BetTitleCode;
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
        assertTrue(dc1x.getBookmakerOdds().get("Bet365") == null);
    }

    @Test
    void mergesBttsYesAndNoFromBothBookmakers() throws Exception {
        OddsApiMarketDto xbet = market("Both Teams To Score", """
                [{"hdp":0,"yes":"1.920","no":"1.865"}]
                """);
        OddsApiMarketDto bet365 = market("Both Teams To Score", """
                [{"yes":"1.950","no":"1.800"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(xbet));
        byBk.put("Bet365", List.of(bet365));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet")),
                OddsMatchContext.of("Korea Republic", "Czech Republic")
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
        OddsLineRow no = btts.getRows().stream()
                .filter(r -> "NO".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("1.920", yes.getBookmakerOdds().get("1xbet"));
        assertEquals("1.950", yes.getBookmakerOdds().get("Bet365"));
        assertEquals("1.865", no.getBookmakerOdds().get("1xbet"));
        assertEquals("1.800", no.getBookmakerOdds().get("Bet365"));
    }

    @Test
    void mergesBttsHalfVariantsIntoSameGroup() throws Exception {
        OddsApiMarketDto ht = market("Both Teams To Score HT", """
                [{"yes":"3.100","no":"1.340"}]
                """);
        OddsApiMarketDto second = market("Both Teams To Score 2H", """
                [{"yes":"2.800","no":"1.400"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("Bet365", List.of(ht, second));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365")),
                OddsMatchContext.of("Korea Republic", "Czech Republic")
        );

        OddsMarketGroup btts = groups.stream()
                .filter(g -> "BTTS".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(4, btts.getRows().size());
        assertTrue(btts.getRows().stream().anyMatch(r -> "YES_1H".equals(r.getSelectionCode())));
        assertTrue(btts.getRows().stream().anyMatch(r -> "NO_2H".equals(r.getSelectionCode())));
    }

    @Test
    void parsesHalfTimeResultFromBet365Labels() throws Exception {
        OddsApiMarketDto htResult = market("Half Time Result", """
                [{"label":"1","hdp":1,"under":"3.400"},{"label":"Draw","under":"2.050"},{"label":"2","hdp":2,"under":"3.400"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("Bet365", List.of(htResult));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365")),
                OddsMatchContext.of("Korea Republic", "Czech Republic")
        );

        OddsMarketGroup ht = groups.stream()
                .filter(g -> "MATCH_RESULT".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, ht.getRows().size());
        assertEquals("HOME", ht.getRows().get(0).getSelectionCode());
        assertEquals("DRAW", ht.getRows().get(1).getSelectionCode());
        assertEquals("AWAY", ht.getRows().get(2).getSelectionCode());
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
        assertEquals("0-0", cs.getRows().get(0).getSelectionCode());
        assertEquals("1-0", cs.getRows().get(1).getSelectionCode());
        assertEquals("2-1", cs.getRows().get(2).getSelectionCode());
    }

    @Test
    void sortsCorrectScoreByTotalGoalsAscending() throws Exception {
        OddsApiMarketDto bet365 = market("Correct Score", """
                [{"label":"1-1","over":"7.00"},{"label":"2-2","over":"19.00"},{"label":"3-1","over":"19.00"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = Map.of("Bet365", List.of(bet365));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365")),
                OddsMatchContext.of("USA", "Paraguay")
        );

        OddsMarketGroup cs = groups.stream()
                .filter(g -> "CORRECT_SCORE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("1-1", "2-2", "3-1"),
                cs.getRows().stream().map(OddsLineRow::getSelectionCode).toList());
    }

    @Test
    void mapsTotalsHalfLineZeroGoalsToCorrectScore() throws Exception {
        OddsApiMarketDto totals = market("Totals", """
                [{"hdp":0.5,"over":"1.025","under":"7.700"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = Map.of("1xbet", List.of(totals));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("1xbet")),
                OddsMatchContext.of("USA", "Paraguay")
        );

        assertTrue(groups.stream().noneMatch(g -> "GOALS".equals(g.getCategory())
                && g.getRows().stream().anyMatch(r -> r.getBetTitle() != null
                && r.getBetTitle().getCode() == BetTitleCode.GAME_SCORE_0_0.getCode())));
        OddsMarketGroup cs = groups.stream()
                .filter(g -> "CORRECT_SCORE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertTrue(cs.getRows().stream().anyMatch(r -> "0-0".equals(r.getSelectionCode())));
    }

    @Test
    void sortsBttsHalfVariantsFirstHalfBeforeSecondHalf() throws Exception {
        OddsApiMarketDto full = market("Both Teams To Score", """
                [{"yes":"2.000","no":"1.754"}]
                """);
        OddsApiMarketDto second = market("Both Teams To Score 2H", """
                [{"yes":"4.000","no":"1.222"}]
                """);
        OddsApiMarketDto first = market("Both Teams To Score HT", """
                [{"yes":"5.500","no":"1.142"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = Map.of(
                "Bet365", List.of(full, second, first)
        );

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365")),
                OddsMatchContext.of("USA", "Paraguay")
        );

        OddsMarketGroup btts = groups.stream()
                .filter(g -> "BTTS".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("YES", "YES_1H", "YES_2H", "NO", "NO_1H", "NO_2H"),
                btts.getRows().stream().map(OddsLineRow::getSelectionCode).toList());
    }

    @Test
    void oddsApiDoesNotProduceHandicapGroup() throws Exception {
        OddsApiMarketDto spread = market("Spread", """
                [
                  {"hdp":-1,"home":"1.790","away":"2.030"},
                  {"hdp":-1.5,"home":"5.550","away":"1.089"}
                ]
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

        org.junit.jupiter.api.Assertions.assertTrue(groups.stream()
                .noneMatch(g -> "HANDICAP".equals(g.getCategory())));
    }

    @Test
    void keepsCorrectScoreLineOnlyFromOneBookmaker() throws Exception {
        OddsApiMarketDto xbetCs = market("Correct Score", """
                [{"label":"3-0","over":"9.500"},{"label":"0-1","over":"15.000"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = Map.of("1xbet", List.of(xbetCs));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("1xbet")),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        OddsMarketGroup cs = groups.stream()
                .filter(g -> "CORRECT_SCORE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, cs.getRows().size());
        assertTrue(cs.getRows().stream().anyMatch(r -> "3-0".equals(r.getSelectionCode())));
        assertTrue(cs.getRows().stream().anyMatch(r -> "0-1".equals(r.getSelectionCode())));
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
