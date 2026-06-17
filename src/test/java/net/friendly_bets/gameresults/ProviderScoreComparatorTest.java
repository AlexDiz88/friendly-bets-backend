package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderScoreComparatorTest {

    @Test
    void matchesRegularScoreFromScreenshotScenario() {
        GameScore primary = GameScore.builder().fullTime("3:1").firstTime("1:0").build();
        GameScore secondary = GameScore.builder().fullTime("3:1").firstTime("1:0").build();

        assertTrue(ProviderScoreComparator.matches(
                primary,
                secondary,
                CanonicalScoreNormalizer.DURATION_REGULAR,
                CanonicalScoreNormalizer.DURATION_REGULAR
        ));
    }

    @Test
    void ignoresMissingOvertimeForRegularMatch() {
        GameScore primary = GameScore.builder().fullTime("2:1").firstTime("1:0").overTime("1:0").build();
        GameScore secondary = GameScore.builder().fullTime("2:1").firstTime("1:0").build();

        assertTrue(ProviderScoreComparator.matches(
                primary,
                secondary,
                CanonicalScoreNormalizer.DURATION_REGULAR,
                CanonicalScoreNormalizer.DURATION_REGULAR
        ));
    }

    @Test
    void requiresOvertimeAndPenaltyForKnockout() {
        GameScore primary = GameScore.builder()
                .fullTime("2:2")
                .firstTime("0:1")
                .overTime("0:0")
                .penalty("4:3")
                .build();
        GameScore secondary = GameScore.builder()
                .fullTime("2:2")
                .firstTime("0:1")
                .overTime("0:0")
                .penalty("4:2")
                .build();

        assertFalse(ProviderScoreComparator.matches(
                primary,
                secondary,
                CanonicalScoreNormalizer.DURATION_PENALTY_SHOOTOUT,
                CanonicalScoreNormalizer.DURATION_PENALTY_SHOOTOUT
        ));
    }

    @Test
    void rejectsFirstTimeMismatch() {
        GameScore primary = GameScore.builder().fullTime("0:2").firstTime("0:1").build();
        GameScore secondary = GameScore.builder().fullTime("0:2").build();

        assertFalse(ProviderScoreComparator.matches(
                primary,
                secondary,
                CanonicalScoreNormalizer.DURATION_REGULAR,
                CanonicalScoreNormalizer.DURATION_REGULAR
        ));
    }
}
