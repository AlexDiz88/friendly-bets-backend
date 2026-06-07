package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsHalfCorrectScoreSubgroupSplitter;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsResultTotalSubgroupSplitter;
import net.friendly_bets.oddsapi.OddsSelectionCode;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetBetTitleMapperMarketsTest {

    private final MarathonbetBetTitleMapper mapper = new MarathonbetBetTitleMapper();

    @Test
    void matchResultOrderIsHomeDrawAway() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_R")
                .name("Исход матча")
                .selections(List.of(
                        sel("Ничья", "3.20"),
                        sel("Корея (победа)", "2.69"),
                        sel("Чехия (победа)", "2.875")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(List.of(market))
                .handicapMarkets(List.of())
                .totalMarkets(List.of())
                .correctScoreMarkets(List.of())
                .doubleChanceMarkets(List.of())
                .resultTotalMarkets(List.of())
                .build();
        OddsMergeResult merged = OddsMerger.merge(mapper.map(markets, "Корея", "Чехия"));
        OddsMarketGroup group = merged.getMarketGroups().stream()
                .filter(g -> OddsMarketCategory.MATCH_RESULT.name().equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("HOME", "DRAW", "AWAY"), group.getRows().stream()
                .map(OddsLineRow::getSelectionCode)
                .toList());
    }

    @Test
    void totalsSortByLineThenUnderBeforeOver() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_TTLG")
                .name("Тотал голов")
                .selections(List.of(
                        sel("Больше 2.5", "1.90"),
                        sel("Меньше 2.5", "1.85"),
                        sel("Больше 1.5", "1.40"),
                        sel("Меньше 1.5", "2.80")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(List.of())
                .handicapMarkets(List.of())
                .totalMarkets(List.of(market))
                .correctScoreMarkets(List.of())
                .doubleChanceMarkets(List.of())
                .resultTotalMarkets(List.of())
                .build();
        OddsMergeResult merged = OddsMerger.merge(mapper.map(markets, "Корея", "Чехия"));
        OddsMarketGroup group = merged.getMarketGroups().stream()
                .filter(g -> OddsMarketCategory.TOTALS.name().equals(g.getCategory()))
                .findFirst()
                .orElseThrow();
        List<String> keys = group.getRows().stream()
                .map(r -> r.getLine() + ":" + r.getSelectionCode())
                .toList();
        assertEquals(List.of("1.5:UNDER", "1.5:OVER", "2.5:UNDER", "2.5:OVER"), keys);
        assertTrue(OddsSelectionCode.UNDER.orderWithinGroup(OddsMarketCategory.TOTALS)
                < OddsSelectionCode.OVER.orderWithinGroup(OddsMarketCategory.TOTALS));
    }

    @Test
    void mapsDoubleChanceFromMarathonResultMarket() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_DC")
                .name("Результат")
                .selections(List.of(
                        sel("1X", "1.46"),
                        sel("12", "1.39"),
                        sel("X2", "1.51")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(List.of())
                .handicapMarkets(List.of())
                .totalMarkets(List.of())
                .correctScoreMarkets(List.of())
                .doubleChanceMarkets(List.of(market))
                .resultTotalMarkets(List.of())
                .build();
        List<MappedOddsQuote> quotes = mapper.map(markets, "Корея", "Чехия");
        assertEquals(3, quotes.size());
        assertTrue(quotes.stream().allMatch(q -> q.getCategory() == OddsMarketCategory.DOUBLE_CHANCE));
        OddsMergeResult merged = OddsMerger.merge(quotes);
        OddsMarketGroup group = merged.getMarketGroups().get(0);
        assertEquals(List.of("DC_1X", "DC_12", "DC_X2"), group.getRows().stream()
                .map(OddsLineRow::getSelectionCode)
                .toList());
    }

    @Test
    void mapsResultTotalYesOnlyAndSplitsIntoSubgroups() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_T1WOV")
                .name("Результат матча + тотал голов 2.5")
                .selections(List.of(
                        sel("Да", "2.10"),
                        sel("Нет", "1.65")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(List.of())
                .handicapMarkets(List.of())
                .totalMarkets(List.of())
                .correctScoreMarkets(List.of())
                .doubleChanceMarkets(List.of())
                .resultTotalMarkets(List.of(market))
                .build();
        List<MappedOddsQuote> quotes = mapper.map(markets, "Корея", "Чехия");
        assertEquals(1, quotes.size());
        assertEquals(BetTitleCode.HOME_WIN_AND_OVER_2_5, BetTitleCode.fromCode(quotes.get(0).getBetTitle().getCode()));
        assertEquals(OddsMarketCategory.RESULT_TOTAL_OVER, quotes.get(0).getCategory());

        OddsMergeResult merged = OddsMerger.merge(quotes);
        List<OddsMarketGroup> groups = new ArrayList<>(merged.getMarketGroups());
        OddsResultTotalSubgroupSplitter.splitIntoSubgroups(groups);
        OddsMarketGroup parent = groups.stream()
                .filter(g -> "resultTotal".equals(g.getGroupKey()))
                .findFirst()
                .orElseThrow();
        assertTrue(parent.getSubgroups() != null && !parent.getSubgroups().isEmpty());
        OddsMarketGroup p1Over = parent.getSubgroups().stream()
                .filter(g -> "resultTotalP1Over".equals(g.getGroupKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, p1Over.getRows().size());
        assertEquals("2.5", p1Over.getRows().get(0).getLine());
    }

    @Test
    void mapsFirstHalfResultTo2001Codes() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_R1")
                .name("Результат, 1-й тайм")
                .selections(List.of(
                        sel("Мексика (победа)", "2.10"),
                        sel("Ничья", "2.00"),
                        sel("ЮАР (победа)", "3.50")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .halfTimeResultMarkets(List.of(market))
                .build();
        List<MappedOddsQuote> quotes = mapper.map(markets, "Мексика", "ЮАР");
        assertEquals(3, quotes.size());
        assertEquals(BetTitleCode.FIRST_HALF_HOME_WIN, code(quotes.get(0)));
        assertEquals(BetTitleCode.FIRST_HALF_DRAW, code(quotes.get(1)));
        assertEquals(BetTitleCode.FIRST_HALF_AWAY_WIN, code(quotes.get(2)));
        assertTrue(quotes.stream().allMatch(q -> q.getCategory() == OddsMarketCategory.MATCH_RESULT));
    }

    @Test
    void mapsCleanWinAndTeamTotal() {
        MarathonbetMarketDto clean = MarathonbetMarketDto.builder()
                .model("MTCH_T1W0")
                .name("Победа с сухим счетом")
                .selections(List.of(sel("Да", "4.50"), sel("Нет", "1.15")))
                .build();
        MarathonbetMarketDto teamTotal = MarathonbetMarketDto.builder()
                .model("MTCH_T1TTLG")
                .name("Тотал голов (Мексика)")
                .selections(List.of(sel("Меньше 1.5", "2.00"), sel("Больше 1.5", "1.75")))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .cleanWinMarkets(List.of(clean))
                .teamTotalHomeMarkets(List.of(teamTotal))
                .build();
        List<MappedOddsQuote> quotes = mapper.map(markets, "Мексика", "ЮАР");
        assertTrue(quotes.stream().anyMatch(q ->
                BetTitleCode.CLEAN_WIN_HOME == code(q) && "YES".equals(q.getSelectionCode())));
        assertTrue(quotes.stream().anyMatch(q ->
                BetTitleCode.HOME_TEAM_OVER_1_5 == BetTitleCode.fromCode(q.getBetTitle().getCode())));
    }

    @Test
    void mapsFirstHalfCorrectScoreTo2201Codes() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_CSW1DYN")
                .name("Точный счет, 1-й тайм")
                .selections(List.of(
                        sel("1:0", "8.50"),
                        sel("0:0", "3.20"),
                        sel("1:1", "7.00")
                ))
                .build();
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .firstHalfCorrectScoreMarkets(List.of(market))
                .build();
        List<MappedOddsQuote> quotes = mapper.map(markets, "Мексика", "ЮАР");
        assertEquals(3, quotes.size());
        assertTrue(quotes.stream().allMatch(q -> q.getCategory() == OddsMarketCategory.FIRST_HALF_CORRECT_SCORE));
        assertEquals(BetTitleCode.FIRST_HALF_SCORE_1_0, code(quotes.get(0)));
        assertEquals(BetTitleCode.FIRST_HALF_SCORE_0_0, code(quotes.get(1)));
        assertEquals(BetTitleCode.FIRST_HALF_SCORE_1_1, code(quotes.get(2)));

        OddsMergeResult merged = OddsMerger.merge(quotes);
        OddsHalfCorrectScoreSubgroupSplitter.splitIntoSubgroups(merged.getMarketGroups());
        OddsMarketGroup parent = merged.getMarketGroups().stream()
                .filter(g -> "halfCorrectScore".equals(g.getGroupKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, parent.getSubgroups().size());
        assertEquals("firstHalfCorrectScore", parent.getSubgroups().get(0).getGroupKey());
        assertEquals(3, parent.getSubgroups().get(0).getRows().size());
    }

    private static BetTitleCode code(MappedOddsQuote quote) {
        return BetTitleCode.fromCode(quote.getBetTitle().getCode());
    }

    private static MarathonbetMarketSelectionDto sel(String name, String odds) {
        return MarathonbetMarketSelectionDto.builder()
                .name(name)
                .odds(new BigDecimal(odds))
                .build();
    }
}
