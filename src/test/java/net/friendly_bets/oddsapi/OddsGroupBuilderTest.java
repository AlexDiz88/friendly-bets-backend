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
                .filter(g -> "HALF_TIME_RESULT".equals(g.getCategory()))
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
        assertEquals("0-0", cs.getRows().get(0).getDisplayLabel());
        assertEquals("1-0", cs.getRows().get(1).getDisplayLabel());
        assertEquals("0-1", cs.getRows().get(2).getDisplayLabel());
    }

    @Test
    void mergesHandicapAcrossMarketNamesWithInvertedAwayLine() throws Exception {
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

        OddsLineRow home = handicap.get().getRows().stream()
                .filter(r -> "HOME".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();
        OddsLineRow away = handicap.get().getRows().stream()
                .filter(r -> "AWAY".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("-1", home.getLine());
        assertEquals("1", away.getLine());
        assertEquals("Ф1 (-1)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, home));
        assertEquals("Ф2 (+1)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, away));
    }

    @Test
    void includesWholeNumberSpreadLinesSortedByAbsLine() throws Exception {
        OddsApiMarketDto spread = market("Spread", """
                [
                  {"hdp":-1.5,"home":"5.550","away":"1.089"},
                  {"hdp":-1,"home":"4.650","away":"1.148"},
                  {"hdp":0,"home":"1.900","away":"1.900"},
                  {"hdp":1,"home":"1.144","away":"4.700"},
                  {"hdp":1.5,"home":"1.087","away":"5.600"}
                ]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(spread));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("1xbet")),
                OddsMatchContext.of("Korea Republic", "Czech Republic")
        );

        OddsMarketGroup handicap = groups.stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(10, handicap.getRows().size());

        OddsLineRow withInteger = handicap.getRows().stream()
                .filter(r -> "HOME".equals(r.getSelectionCode()) && "-1".equals(r.getLine()))
                .findFirst()
                .orElseThrow();
        assertEquals("4.650", withInteger.getBookmakerOdds().get("1xbet"));

        OddsLineRow awayPlusOne = handicap.getRows().stream()
                .filter(r -> "AWAY".equals(r.getSelectionCode()) && "1".equals(r.getLine()))
                .findFirst()
                .orElseThrow();
        assertEquals("1.148", awayPlusOne.getBookmakerOdds().get("1xbet"));

        OddsLineRow awayMinusOne = handicap.getRows().stream()
                .filter(r -> "AWAY".equals(r.getSelectionCode()) && "-1".equals(r.getLine()))
                .findFirst()
                .orElseThrow();
        assertEquals("4.700", awayMinusOne.getBookmakerOdds().get("1xbet"));

        List<String> labels = handicap.getRows().stream()
                .map(r -> OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, r))
                .toList();
        assertEquals(List.of(
                "Ф1 (0)",
                "Ф2 (0)",
                "Ф1 (-1)",
                "Ф2 (-1)",
                "Ф1 (+1)",
                "Ф2 (+1)",
                "Ф1 (-1.5)",
                "Ф2 (-1.5)",
                "Ф1 (+1.5)",
                "Ф2 (+1.5)"
        ), labels);
    }

    @Test
    void ignoresSwappedAwayOddsFromSecondBookmakerOnPlusHandicap() throws Exception {
        OddsApiMarketDto spread = market("Spread", """
                [{"hdp":-1,"home":"4.650","away":"1.148"}]
                """);
        OddsApiMarketDto swapped = market("Alternative Asian Handicap", """
                [{"hdp":-1,"home":"1.148","away":"4.650"}]
                """);

        Map<String, List<OddsApiMarketDto>> byBk = new LinkedHashMap<>();
        byBk.put("1xbet", List.of(spread));
        byBk.put("Bet365", List.of(swapped));

        List<OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("Bet365", "1xbet")),
                OddsMatchContext.of("Korea Republic", "Czech Republic")
        );

        OddsMarketGroup handicap = groups.stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        OddsLineRow awayPlusOne = handicap.getRows().stream()
                .filter(r -> "AWAY".equals(r.getSelectionCode()) && "1".equals(r.getLine()))
                .findFirst()
                .orElseThrow();

        assertEquals("1.148", awayPlusOne.getBookmakerOdds().get("1xbet"));
        org.junit.jupiter.api.Assertions.assertNull(awayPlusOne.getBookmakerOdds().get("Bet365"));

        OddsSelectionKey.applyBestOdds(awayPlusOne, OddsMarketCategory.HANDICAP);
        assertEquals("1.148", awayPlusOne.getBestOdds());
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
