package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsResultTotalCalculatorTest {

    @Test
    void parsesUnderComboTotalLineCorrectly() {
        OddsResultTotalCombo combo = OddsResultTotalCombo.fromBetTitleCode(BetTitleCode.HOME_WIN_AND_UNDER_2_5);
        assertNotNull(combo);
        assertTrue(Math.abs(combo.totalLine() - 2.5) < 1e-9);
    }

    @Test
    void buildsSeparateOverAndUnderGroups() {
        List<OddsMarketGroup> merged = List.of(
                matchResultGroup(),
                totalsGroup("2.5", "2.29", "1.64")
        );

        List<OddsMarketGroup> groups = OddsResultTotalCalculator.buildGroups(merged, List.of("bet365", "1xbet"));
        assertFalse(groups.isEmpty());

        OddsMarketGroup over = findCategory(groups, OddsMarketCategory.RESULT_TOTAL_OVER).orElseThrow();
        OddsMarketGroup under = findCategory(groups, OddsMarketCategory.RESULT_TOTAL_UNDER).orElseThrow();

        assertTrue(over.getRows().stream().anyMatch(r ->
                r.getBetTitle().getCode() == BetTitleCode.HOME_WIN_AND_OVER_2_5.getCode()));
        assertTrue(under.getRows().stream().anyMatch(r ->
                r.getBetTitle().getCode() == BetTitleCode.HOME_WIN_AND_UNDER_2_5.getCode()));
        assertTrue(over.getRows().size() == under.getRows().size(),
                "over=" + over.getRows().size() + " under=" + under.getRows().size());
        assertTrue(over.getRows().stream().noneMatch(r -> r.getBetTitle().getLabel().contains("ТМ")));
        assertTrue(under.getRows().stream().noneMatch(r -> r.getBetTitle().getLabel().contains("ТБ")));
    }

    /**
     * Пример: США (хоз) 2.03; ТБ 2.5 = 2.29. У БК «П1 + ТБ 2.5» ≈ 3.48.
     */
    @Test
    void calibratesHomeWinOver25WithinReasonableBand() {
        List<OddsMarketGroup> groups = OddsResultTotalCalculator.buildGroups(
                List.of(matchResultGroup(), totalsGroup("2.5", "2.29", "1.64")),
                List.of("bk"));

        double odds = findOdds(groups, OddsMarketCategory.RESULT_TOTAL_OVER, BetTitleCode.HOME_WIN_AND_OVER_2_5);
        assertNotNull(odds);
        double bookmakerRef = 3.48;
        assertTrue(odds < 2.03 * 2.29, "combo should be below naive product");
        assertTrue(Math.abs(odds - bookmakerRef) / bookmakerRef <= 0.15);
    }

    /**
     * Эталон с линии БК (Канада П1 + тотал), кэфы merged 1X2/тоталов — порядок величины.
     */
    @Test
    void canadaStyleHomeWinCombosCloserToBookmaker() {
        List<OddsMarketGroup> groups = OddsResultTotalCalculator.buildGroups(
                List.of(
                        matchResultGroupCanada(),
                        totalsGroupMulti()
                ),
                List.of("bk"));

        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_OVER, BetTitleCode.HOME_WIN_AND_OVER_1_5, 2.39);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_OVER, BetTitleCode.HOME_WIN_AND_OVER_2_5, 3.20);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_OVER, BetTitleCode.HOME_WIN_AND_OVER_3_5, 6.40);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_OVER, BetTitleCode.HOME_WIN_AND_OVER_4_5, 11.00);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_UNDER, BetTitleCode.HOME_WIN_AND_UNDER_1_5, 5.65);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_UNDER, BetTitleCode.HOME_WIN_AND_UNDER_2_5, 3.48);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_UNDER, BetTitleCode.HOME_WIN_AND_UNDER_3_5, 2.29);
        assertWithinBand(groups, OddsMarketCategory.RESULT_TOTAL_UNDER, BetTitleCode.HOME_WIN_AND_UNDER_4_5, 1.99);
    }

    private static void assertWithinBand(
            List<OddsMarketGroup> groups,
            OddsMarketCategory category,
            BetTitleCode code,
            double bookmakerRef
    ) {
        double odds = findOdds(groups, category, code);
        assertNotNull(odds, code.name());
        double relErr = Math.abs(odds - bookmakerRef) / bookmakerRef;
        assertTrue(relErr <= 0.15,
                code.name() + " odds " + odds + " vs BK " + bookmakerRef + " err=" + relErr);
    }

    @Test
    void calibratesHomeWinUnder25WithinReasonableBand() {
        List<OddsMarketGroup> groups = OddsResultTotalCalculator.buildGroups(
                List.of(matchResultGroup(), totalsGroup("2.5", "2.29", "1.64")),
                List.of("bk"));

        double odds = findOdds(groups, OddsMarketCategory.RESULT_TOTAL_UNDER, BetTitleCode.HOME_WIN_AND_UNDER_2_5);
        assertNotNull(odds);
        double bookmakerRef = 3.72;
        assertTrue(odds > 2.03 * 1.64, "combo should be above naive product");
        assertTrue(Math.abs(odds - bookmakerRef) / bookmakerRef <= 0.15);
    }

    private static Double findOdds(
            List<OddsMarketGroup> groups,
            OddsMarketCategory category,
            BetTitleCode code
    ) {
        return findCategory(groups, category)
                .flatMap(g -> g.getRows().stream()
                        .filter(r -> r.getBetTitle().getCode() == code.getCode())
                        .map(r -> Double.parseDouble(r.getBestOdds()))
                        .findFirst())
                .orElse(null);
    }

    private static Optional<OddsMarketGroup> findCategory(List<OddsMarketGroup> groups, OddsMarketCategory category) {
        return groups.stream()
                .filter(g -> category.name().equals(g.getCategory()))
                .findFirst();
    }

    private static OddsMarketGroup matchResultGroupCanada() {
        return OddsMarketGroup.builder()
                .category(OddsMarketCategory.MATCH_RESULT.name())
                .groupKey("matchResult")
                .sortOrder(1)
                .rows(List.of(
                        row("HOME", null, "2.00"),
                        row("DRAW", null, "3.40"),
                        row("AWAY", null, "4.20")
                ))
                .build();
    }

    private static OddsMarketGroup totalsGroupMulti() {
        return OddsMarketGroup.builder()
                .category(OddsMarketCategory.TOTALS.name())
                .groupKey("totals")
                .sortOrder(4)
                .rows(List.of(
                        row("OVER", "1.5", "1.35"),
                        row("UNDER", "1.5", "3.10"),
                        row("OVER", "2.5", "2.05"),
                        row("UNDER", "2.5", "1.75"),
                        row("OVER", "3.5", "3.50"),
                        row("UNDER", "3.5", "1.28"),
                        row("OVER", "4.5", "6.50"),
                        row("UNDER", "4.5", "1.10")
                ))
                .build();
    }

    private static OddsMarketGroup matchResultGroup() {
        return OddsMarketGroup.builder()
                .category(OddsMarketCategory.MATCH_RESULT.name())
                .groupKey("matchResult")
                .sortOrder(1)
                .rows(List.of(
                        row("HOME", null, "2.03"),
                        row("DRAW", null, "3.50"),
                        row("AWAY", null, "3.94")
                ))
                .build();
    }

    private static OddsMarketGroup totalsGroup(String line, String over, String under) {
        return OddsMarketGroup.builder()
                .category(OddsMarketCategory.TOTALS.name())
                .groupKey("totals")
                .sortOrder(4)
                .rows(List.of(
                        row("OVER", line, over),
                        row("UNDER", line, under)
                ))
                .build();
    }

    private static OddsLineRow row(String selection, String line, String odds) {
        Map<String, String> bk = new LinkedHashMap<>();
        bk.put("bk", odds);
        return OddsLineRow.builder()
                .selectionCode(selection)
                .line(line)
                .bestOdds(odds)
                .bestBookmaker("bk")
                .bookmakerOdds(bk)
                .build();
    }
}
