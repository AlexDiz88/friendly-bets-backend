package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsMergerTest {

    @Test
    void sortsDoubleChanceByPeriodThenOutcome() {
        List<MappedOddsQuote> quotes = List.of(
                dcQuote(BetTitleCode.SECOND_HALF_AWAY_WIN_OR_DRAW, "DC_X2", "1.95"),
                dcQuote(BetTitleCode.FIRST_HALF_HOME_OR_AWAY_WIN, "DC_12", "1.58"),
                dcQuote(BetTitleCode.HOME_WIN_OR_DRAW, "DC_1X", "1.082"),
                dcQuote(BetTitleCode.SECOND_HALF_HOME_WIN_OR_DRAW, "DC_1X", "1.051"),
                dcQuote(BetTitleCode.FIRST_HALF_AWAY_WIN_OR_DRAW, "DC_X2", "1.727"),
                dcQuote(BetTitleCode.HOME_OR_AWAY_WIN, "DC_12", "1.24"),
                dcQuote(BetTitleCode.FIRST_HALF_HOME_WIN_OR_DRAW, "DC_1X", "1.042"),
                dcQuote(BetTitleCode.AWAY_WIN_OR_DRAW, "DC_X2", "2.93"),
                dcQuote(BetTitleCode.SECOND_HALF_HOME_OR_AWAY_WIN, "DC_12", "1.42")
        );

        OddsMergeResult result = OddsMerger.merge(quotes);
        OddsMarketGroup dc = result.getMarketGroups().stream()
                .filter(g -> "DOUBLE_CHANCE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(
                BetTitleCode.HOME_WIN_OR_DRAW,
                BetTitleCode.HOME_OR_AWAY_WIN,
                BetTitleCode.AWAY_WIN_OR_DRAW,
                BetTitleCode.FIRST_HALF_HOME_WIN_OR_DRAW,
                BetTitleCode.FIRST_HALF_HOME_OR_AWAY_WIN,
                BetTitleCode.FIRST_HALF_AWAY_WIN_OR_DRAW,
                BetTitleCode.SECOND_HALF_HOME_WIN_OR_DRAW,
                BetTitleCode.SECOND_HALF_HOME_OR_AWAY_WIN,
                BetTitleCode.SECOND_HALF_AWAY_WIN_OR_DRAW
        ), dc.getRows().stream()
                .map(r -> BetTitleCode.fromCode(r.getBetTitle().getCode()))
                .toList());
    }

    private static MappedOddsQuote dcQuote(BetTitleCode code, String selectionCode, String odds) {
        BetTitle title = BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(false)
                .build();
        return MappedOddsQuote.builder()
                .bookmaker("Marathonbet")
                .marketName("Double Chance")
                .category(OddsMarketCategory.DOUBLE_CHANCE)
                .betTitle(title)
                .odds(odds)
                .mappingStatus(OddsMappingStatus.OK)
                .selectionCode(selectionCode)
                .build();
    }

    @Test
    void picksHigherOddsAcrossBookmakers() {
        BetTitle title = BetTitle.builder()
                .code(BetTitleCode.HOME_WIN.getCode())
                .label(BetTitleCode.HOME_WIN.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker("1xbet")
                        .marketName("ML")
                        .category(OddsMarketCategory.MATCH_RESULT)
                        .betTitle(title)
                        .odds("2.10")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("HOME")
                        .build(),
                MappedOddsQuote.builder()
                        .bookmaker("Bet365")
                        .marketName("ML")
                        .category(OddsMarketCategory.MATCH_RESULT)
                        .betTitle(title)
                        .odds("2.20")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("HOME")
                        .build()
        );

        OddsMergeResult result = OddsMerger.merge(quotes);
        OddsMarketGroup mr = result.getMarketGroups().stream()
                .filter(g -> "MATCH_RESULT".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        OddsLineRow home = mr.getRows().stream()
                .filter(r -> "HOME".equals(r.getSelectionCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("2.20", home.getBestOdds());
        assertEquals("Bet365", home.getBestBookmaker());
    }

    @Test
    void mergesSameBetTitleFromDifferentMarketNames() {
        BetTitle awayMinusOne = BetTitle.builder()
                .code(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode())
                .label(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker("1xbet")
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(awayMinusOne)
                        .odds("4.700")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("-1")
                        .build(),
                MappedOddsQuote.builder()
                        .bookmaker("Bet365")
                        .marketName("Alternative Asian Handicap")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(awayMinusOne)
                        .odds("4.650")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("-1")
                        .build()
        );

        OddsMergeResult result = OddsMerger.merge(quotes);
        assertTrue(result.getMismatches().isEmpty());
        OddsLineRow row = result.getMarketGroups().stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .findFirst()
                .orElseThrow()
                .getRows().get(0);
        assertEquals("4.700", row.getBookmakerOdds().get("1xbet"));
        assertEquals("4.650", row.getBookmakerOdds().get("Bet365"));
        assertEquals("4.700", row.getBestOdds());
    }

    @Test
    void keepsRowWhenOnlyOneBookmakerHasBetTitle() {
        BetTitle score = BetTitle.builder()
                .code(BetTitleCode.GAME_SCORE_3_0.getCode())
                .label(BetTitleCode.GAME_SCORE_3_0.getLabel())
                .isNot(false)
                .build();

        OddsMergeResult result = OddsMerger.merge(List.of(
                MappedOddsQuote.builder()
                        .bookmaker("1xbet")
                        .marketName("Correct Score")
                        .category(OddsMarketCategory.CORRECT_SCORE)
                        .betTitle(score)
                        .odds("9.500")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("3-0")
                        .build()
        ));

        assertTrue(result.getMismatches().isEmpty());
        assertEquals(1, result.getMarketGroups().stream()
                .filter(g -> "CORRECT_SCORE".equals(g.getCategory()))
                .findFirst()
                .orElseThrow()
                .getRows().size());
    }

    @Test
    void excludesCrossBookmakerMismatch() {
        BetTitle title = BetTitle.builder()
                .code(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getCode())
                .label(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker("1xbet")
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(title)
                        .odds("1.15")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("1")
                        .build(),
                MappedOddsQuote.builder()
                        .bookmaker("Bet365")
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(title)
                        .odds("4.65")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("1")
                        .build()
        );

        OddsMergeResult result = OddsMerger.merge(quotes);
        assertTrue(result.getMismatches().size() >= 1);
        assertTrue(result.getMarketGroups().stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .flatMap(g -> g.getRows().stream())
                .noneMatch(r -> r.getBetTitle() != null
                        && r.getBetTitle().getCode() == title.getCode()));
    }

    @Test
    void includesCrossBookmakerMismatchWhenDemoMode() {
        BetTitle title = BetTitle.builder()
                .code(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getCode())
                .label(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker("1xbet")
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(title)
                        .odds("1.15")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("1")
                        .build(),
                MappedOddsQuote.builder()
                        .bookmaker("Bet365")
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(title)
                        .odds("4.65")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("1")
                        .build()
        );

        OddsMergeResult result = OddsMerger.merge(quotes, true);
        assertTrue(result.getMismatches().size() >= 1);
        OddsLineRow row = result.getMarketGroups().stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .flatMap(g -> g.getRows().stream())
                .filter(r -> r.getBetTitle() != null && r.getBetTitle().getCode() == title.getCode())
                .findFirst()
                .orElseThrow();
        assertEquals("1.15", row.getBookmakerOdds().get("1xbet"));
        assertEquals("4.65", row.getBookmakerOdds().get("Bet365"));
        assertTrue(row.isCrossBookmakerMismatch());
    }

    @Test
    void sortMarketGroupRowsReordersPlayoffCategoryFromShuffledSnapshot() {
        OddsMarketGroup group = OddsMarketGroup.builder()
                .category(OddsMarketCategory.PLAYOFF_EXTRA_TIME.name())
                .rows(new java.util.ArrayList<>(List.of(
                        playoffRow(BetTitleCode.PLAYOFF_HOME_OR_AWAY_REGULAR, false),
                        playoffRow(BetTitleCode.PLAYOFF_EXTRA_TIME, false),
                        playoffRow(BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE, false),
                        playoffRow(BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE, false),
                        playoffRow(BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES, true)
                )))
                .build();

        OddsMerger.sortMarketGroupRows(List.of(group));

        assertEquals(BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE.getCode(), group.getRows().get(0).getBetTitle().getCode());
        assertEquals(BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE.getCode(), group.getRows().get(1).getBetTitle().getCode());
        assertEquals(BetTitleCode.PLAYOFF_EXTRA_TIME.getCode(), group.getRows().get(2).getBetTitle().getCode());
        assertEquals(BetTitleCode.PLAYOFF_HOME_OR_AWAY_REGULAR.getCode(), group.getRows().get(3).getBetTitle().getCode());
        assertEquals(BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES.getCode(), group.getRows().get(4).getBetTitle().getCode());
    }

    private static OddsLineRow playoffRow(BetTitleCode code, boolean isNot) {
        return OddsLineRow.builder()
                .betTitle(BetTitle.builder()
                        .code(code.getCode())
                        .label(code.getLabel())
                        .isNot(isNot)
                        .build())
                .build();
    }
}
