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
    void recognizesThirdGroupMultiLetterPlaceholders() {
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("3rd Group A/E/H/I/J"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("3rd Group E/H/I/J/K"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("—"));
    }

    @Test
    void recognizesBracketWinnerCodes() {
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("W73"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("RU101"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("2A"));
        assertTrue(FourScorePlayoffPlaceholderNames.isPlaceholder("3ABCDF"));
    }

    @Test
    void doesNotTreatRealTeamsAsPlaceholders() {
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Чехия"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Mexico"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("CzechRepublic"));
        assertFalse(FourScorePlayoffPlaceholderNames.isPlaceholder("Brazil"));
    }
}
