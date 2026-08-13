package net.friendly_bets.models.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BetValueRangeTest {

    @ParameterizedTest(name = "odds {0} -> {1}")
    @DisplayName("fromOdds maps exclusive-lower inclusive-upper buckets; first bucket includes 1.00")
    @CsvSource({
            "1.00, SUPER_LOW",
            "1.50, SUPER_LOW",
            "1.51, LOW",
            "1.80, LOW",
            "1.81, MEDIUM",
            "2.20, MEDIUM",
            "2.21, HIGH",
            "2.50, HIGH",
            "2.51, VERY_HIGH",
            "4.00, VERY_HIGH",
            "4.01, UNLIKELY",
            "7.00, UNLIKELY",
            "7.01, COSMIC",
            "15.00, COSMIC",
            "15.01, UNREALISTIC",
            "50.00, UNREALISTIC"
    })
    void fromOdds_ShouldMapBoundaries(double odds, BetValueRange expected) {
        assertEquals(expected, BetValueRange.fromOdds(odds));
    }
}
