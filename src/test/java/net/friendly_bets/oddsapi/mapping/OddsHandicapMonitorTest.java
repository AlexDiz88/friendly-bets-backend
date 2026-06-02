package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsHandicapMonitorTest {

    @Test
    void detectsDivergenceBetweenXbetSpreadAndBet365AsianHandicap() {
        BetTitle awayMinusOne = BetTitle.builder()
                .code(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode())
                .label(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker(XbetOddsAdapter.BOOKMAKER)
                        .marketName("Spread")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(awayMinusOne)
                        .odds("7.900")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("-1")
                        .build(),
                MappedOddsQuote.builder()
                        .bookmaker(Bet365OddsAdapter.BOOKMAKER)
                        .marketName("Alternative Asian Handicap")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(awayMinusOne)
                        .odds("1.400")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("-1")
                        .build()
        );

        List<OddsCrossBookmakerMismatch> mismatches = OddsHandicapMonitor.detect(quotes);

        assertEquals(1, mismatches.size());
        assertTrue(mismatches.get(0).getOddsA().equals("7.900") || mismatches.get(0).getOddsB().equals("7.900"));
    }

    @Test
    void skipsWhenBetTitleMissingOnPrimaryBookmaker() {
        BetTitle awayPlusOne = BetTitle.builder()
                .code(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getCode())
                .label(BetTitleCode.HANDICAP_AWAY_PLUS_1_0.getLabel())
                .isNot(false)
                .build();

        List<MappedOddsQuote> quotes = List.of(
                MappedOddsQuote.builder()
                        .bookmaker(Bet365OddsAdapter.BOOKMAKER)
                        .marketName("Alternative Asian Handicap")
                        .category(OddsMarketCategory.HANDICAP)
                        .betTitle(awayPlusOne)
                        .odds("1.725")
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode("AWAY")
                        .line("1")
                        .build()
        );

        assertTrue(OddsHandicapMonitor.detect(quotes).isEmpty());
    }
}
