package net.friendly_bets.fourscore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourScorePlayoffPlaceholderNamesTest {

    @Test
    void recognizesEnglishAndRussianGroupPlaceholders() {
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("2nd Group I"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("2-я группа Д"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("2-я группа B"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("1st Group A"));
    }

    @Test
    void doesNotTreatRealTeamsAsPlaceholders() {
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Чехия"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Mexico"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("CzechRepublic"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Brazil"));
    }
}
