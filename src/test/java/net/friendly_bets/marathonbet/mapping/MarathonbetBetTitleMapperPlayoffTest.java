package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetBetTitleMapperPlayoffTest {

    private final MarathonbetBetTitleMapper mapper = new MarathonbetBetTitleMapper();

    @Test
    void mapsPlayoffQualificationAndExtraTimeMarkets() {
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .playoffMarkets(List.of(
                        market("MTCH_QLF", "Выход в 1/8 финала", sel("ЮАР", "3.62"), sel("Канада", "1.30")),
                        market("MTCH_OT", "Дополнительное время", sel("Да", "2.10"), sel("Нет", "1.65")),
                        market("MTCH_PNL_SHT", "Послематчевые пенальти", sel("Да", "3.00"), sel("Нет", "1.35")),
                        market("MTCH_WM_FT1", "Выход в 1/8 финала по результату основного времени - ЮАР",
                                sel("Да", "4.50"), sel("Нет", "1.18")),
                        market("MTCH_WM_PNL", "Выход в 1/8 финала по результату послематчевых пенальти - любая команда",
                                sel("Да", "2.80"), sel("Нет", "1.40"))
                ))
                .build();

        OddsMergeResult merged = OddsMerger.merge(mapper.map(markets, "ЮАР", "Канада"));
        OddsMarketGroup group = merged.getMarketGroups().stream()
                .filter(g -> OddsMarketCategory.PLAYOFF_EXTRA_TIME.name().equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        assertTrue(group.getRows().stream().anyMatch(r -> code(r) == BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE.getCode()));
        assertTrue(group.getRows().stream().anyMatch(r -> code(r) == BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE.getCode()));
        assertTrue(group.getRows().stream().anyMatch(r ->
                code(r) == BetTitleCode.PLAYOFF_EXTRA_TIME.getCode() && !r.getBetTitle().isNot()));
        assertTrue(group.getRows().stream().anyMatch(r ->
                code(r) == BetTitleCode.PLAYOFF_PENALTIES.getCode() && !r.getBetTitle().isNot()));
        assertTrue(group.getRows().stream().anyMatch(r ->
                code(r) == BetTitleCode.PLAYOFF_HOME_WIN_REGULAR.getCode() && !r.getBetTitle().isNot()));
        assertTrue(group.getRows().stream().anyMatch(r ->
                code(r) == BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES.getCode() && !r.getBetTitle().isNot()));

        List<Short> orderedCodes = group.getRows().stream().map(MarathonbetBetTitleMapperPlayoffTest::code).toList();
        assertEquals(BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE.getCode(), orderedCodes.get(0));
        assertEquals(BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE.getCode(), orderedCodes.get(1));
        assertTrue(orderedCodes.indexOf(BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES.getCode())
                > orderedCodes.indexOf(BetTitleCode.PLAYOFF_HOME_WIN_REGULAR.getCode()));
    }

    @Test
    void semiFinalQualificationUsesAdvanceFinalCodes() {
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .playoffMarkets(List.of(
                        market("MTCH_QLF", "Выход в финал", sel("Франция", "1.90"), sel("Германия", "1.95"))
                ))
                .build();

        OddsMergeResult merged = OddsMerger.merge(mapper.map(markets, "Франция", "Германия"));
        OddsMarketGroup group = merged.getMarketGroups().stream()
                .filter(g -> OddsMarketCategory.PLAYOFF_EXTRA_TIME.name().equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                BetTitleCode.PLAYOFF_HOME_ADVANCE_FINAL.getCode(),
                group.getRows().stream()
                        .filter(r -> "HOME".equals(r.getSelectionCode()))
                        .map(MarathonbetBetTitleMapperPlayoffTest::code)
                        .findFirst()
                        .orElseThrow()
        );
    }

    private static short code(OddsLineRow row) {
        return row.getBetTitle().getCode();
    }

    private static MarathonbetMarketDto market(String model, String name, MarathonbetMarketSelectionDto... selections) {
        return MarathonbetMarketDto.builder()
                .model(model)
                .name(name)
                .selections(List.of(selections))
                .build();
    }

    private static MarathonbetMarketSelectionDto sel(String name, String odds) {
        return MarathonbetMarketSelectionDto.builder()
                .name(name)
                .odds(new BigDecimal(odds))
                .build();
    }
}
