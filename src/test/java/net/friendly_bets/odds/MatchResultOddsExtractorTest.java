package net.friendly_bets.odds;

import net.friendly_bets.dto.MatchResultOddsDto;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchResultOddsExtractorTest {

    @Test
    void extractsFullMatchHomeDrawAway_ignoringHalfRows() {
        Odds odds = Odds.builder()
                .matchScheduleId("m1")
                .marketGroups(List.of(
                        OddsMarketGroup.builder()
                                .category(OddsMarketCategory.MATCH_RESULT.name())
                                .rows(List.of(
                                        row("HOME", "1.85", BetTitleCode.HOME_WIN),
                                        row("DRAW", "3.40", BetTitleCode.DRAW),
                                        row("AWAY", "4.20", BetTitleCode.AWAY_WIN),
                                        row("HOME", "2.10", BetTitleCode.FIRST_HALF_HOME_WIN),
                                        row("DRAW", "2.50", BetTitleCode.FIRST_HALF_DRAW),
                                        row("AWAY", "3.80", BetTitleCode.FIRST_HALF_AWAY_WIN),
                                        row("HOME", "2.55", BetTitleCode.SECOND_HALF_HOME_WIN),
                                        row("DRAW", "2.70", BetTitleCode.SECOND_HALF_DRAW),
                                        row("AWAY", "2.90", BetTitleCode.SECOND_HALF_AWAY_WIN)
                                ))
                                .build()
                ))
                .build();

        Optional<MatchResultOddsDto> extracted = MatchResultOddsExtractor.extract(odds);
        assertTrue(extracted.isPresent());
        assertEquals("1.85", extracted.get().getHome());
        assertEquals("3.40", extracted.get().getDraw());
        assertEquals("4.20", extracted.get().getAway());
    }

    @Test
    void emptyWhenOnlyHalfTimeRowsPresent() {
        Odds odds = Odds.builder()
                .matchScheduleId("m1")
                .marketGroups(List.of(
                        OddsMarketGroup.builder()
                                .category(OddsMarketCategory.MATCH_RESULT.name())
                                .rows(List.of(
                                        row("HOME", "2.55", BetTitleCode.SECOND_HALF_HOME_WIN),
                                        row("DRAW", "2.70", BetTitleCode.SECOND_HALF_DRAW),
                                        row("AWAY", "2.90", BetTitleCode.SECOND_HALF_AWAY_WIN)
                                ))
                                .build()
                ))
                .build();

        assertTrue(MatchResultOddsExtractor.extract(odds).isEmpty());
    }

    @Test
    void emptyWhenAnyFullMatchLegMissing() {
        Odds odds = Odds.builder()
                .matchScheduleId("m1")
                .marketGroups(List.of(
                        OddsMarketGroup.builder()
                                .category(OddsMarketCategory.MATCH_RESULT.name())
                                .rows(List.of(
                                        row("HOME", "1.85", BetTitleCode.HOME_WIN),
                                        row("DRAW", "3.40", BetTitleCode.DRAW)
                                ))
                                .build()
                ))
                .build();

        assertTrue(MatchResultOddsExtractor.extract(odds).isEmpty());
    }

    @Test
    void fallsBackToBookmakerOdds() {
        OddsLineRow home = OddsLineRow.builder()
                .selectionCode("HOME")
                .betTitle(betTitle(BetTitleCode.HOME_WIN))
                .bookmakerOdds(Map.of("Marathonbet", "2.10"))
                .build();
        Odds odds = Odds.builder()
                .matchScheduleId("m1")
                .marketGroups(List.of(
                        OddsMarketGroup.builder()
                                .category(OddsMarketCategory.MATCH_RESULT.name())
                                .rows(List.of(
                                        home,
                                        row("DRAW", "3.20", BetTitleCode.DRAW),
                                        row("AWAY", "3.50", BetTitleCode.AWAY_WIN)
                                ))
                                .build()
                ))
                .build();

        Optional<MatchResultOddsDto> extracted = MatchResultOddsExtractor.extract(odds);
        assertTrue(extracted.isPresent());
        assertEquals("2.10", extracted.get().getHome());
    }

    private static OddsLineRow row(String code, String bestOdds, BetTitleCode betTitleCode) {
        return OddsLineRow.builder()
                .selectionCode(code)
                .bestOdds(bestOdds)
                .betTitle(betTitle(betTitleCode))
                .build();
    }

    private static BetTitle betTitle(BetTitleCode code) {
        return BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(false)
                .build();
    }
}
