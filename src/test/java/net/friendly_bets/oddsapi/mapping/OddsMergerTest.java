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
}
