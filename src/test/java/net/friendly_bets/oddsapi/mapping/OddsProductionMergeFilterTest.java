package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsProductionMergeFilterTest {

    @Test
    void includesNonHandicapQuotesFromAnyBookmaker() {
        MappedOddsQuote quote = MappedOddsQuote.builder()
                .bookmaker(Bet365OddsAdapter.BOOKMAKER)
                .category(OddsMarketCategory.MATCH_RESULT)
                .mappingStatus(OddsMappingStatus.OK)
                .odds("2.00")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HOME_WIN.getCode())
                        .label(BetTitleCode.HOME_WIN.getLabel())
                        .isNot(false)
                        .build())
                .build();

        assertTrue(OddsProductionMergeFilter.includeInProductionMerge(quote));
    }

    @Test
    void excludesBet365HandicapFromProductionMerge() {
        MappedOddsQuote quote = MappedOddsQuote.builder()
                .bookmaker(Bet365OddsAdapter.BOOKMAKER)
                .marketName("Alternative Asian Handicap")
                .category(OddsMarketCategory.HANDICAP)
                .mappingStatus(OddsMappingStatus.OK)
                .odds("1.400")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode())
                        .label(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getLabel())
                        .isNot(false)
                        .build())
                .build();

        assertFalse(OddsProductionMergeFilter.includeInProductionMerge(quote));
    }

    @Test
    void excludesXbetHandicapFromProductionMerge() {
        MappedOddsQuote quote = MappedOddsQuote.builder()
                .bookmaker(XbetOddsAdapter.BOOKMAKER)
                .marketName("Spread")
                .category(OddsMarketCategory.HANDICAP)
                .mappingStatus(OddsMappingStatus.OK)
                .odds("4.700")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getCode())
                        .label(BetTitleCode.HANDICAP_AWAY_MINUS_1_0.getLabel())
                        .isNot(false)
                        .build())
                .build();

        assertFalse(OddsProductionMergeFilter.includeInProductionMerge(quote));
    }

    @Test
    void includesMarathonbetPeriodHandicapInProductionMerge() {
        MappedOddsQuote quote = MappedOddsQuote.builder()
                .bookmaker(MarathonbetBookmaker.KEY)
                .marketName("Победа с учетом форы, 2-й тайм")
                .category(OddsMarketCategory.PERIOD_HANDICAP)
                .mappingStatus(OddsMappingStatus.OK)
                .odds("8.80")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.SECOND_HALF_HANDICAP_HOME_MINUS_2_0.getCode())
                        .label(BetTitleCode.SECOND_HALF_HANDICAP_HOME_MINUS_2_0.getLabel())
                        .isNot(false)
                        .build())
                .build();

        assertTrue(OddsProductionMergeFilter.includeInProductionMerge(quote));
    }

    @Test
    void includesMarathonbetHandicapInProductionMerge() {
        MappedOddsQuote quote = MappedOddsQuote.builder()
                .bookmaker(MarathonbetBookmaker.KEY)
                .marketName("Победа с учетом форы")
                .category(OddsMarketCategory.HANDICAP)
                .mappingStatus(OddsMappingStatus.OK)
                .odds("1.52")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HANDICAP_HOME_MINUS_1_0.getCode())
                        .label(BetTitleCode.HANDICAP_HOME_MINUS_1_0.getLabel())
                        .isNot(false)
                        .build())
                .build();

        assertTrue(OddsProductionMergeFilter.includeInProductionMerge(quote));
    }
}
