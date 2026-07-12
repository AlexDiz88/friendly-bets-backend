package net.friendly_bets.utils;

import net.friendly_bets.models.League;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutBetPrivacyStagesTest {

    @ParameterizedTest
    @CsvSource({
            "1/2 [1], 1/2",
            "1/2 [2], 1/2",
            "final, final",
            "third_place, third_place",
            "1/2-s1, 1/2",
            "1/4 [1], 1/4"
    })
    @DisplayName("normalizeStage strips leg suffixes")
    void normalizeStage(String input, String expected) {
        assertEquals(expected, KnockoutBetPrivacyStages.normalizeStage(input));
    }

    @Test
    @DisplayName("sensitive slots for WC CL EC LE semi final third place")
    void isSensitiveKnockoutSlot_sensitiveStages() {
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.WC, "1/2"));
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.WC, "third_place"));
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.WC, "final"));
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.CL, "1/2 [2]"));
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.EC, "final"));
        assertTrue(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.LE, "1/2 [1]"));
    }

    @Test
    @DisplayName("non-sensitive leagues and earlier knockout rounds")
    void isSensitiveKnockoutSlot_notSensitive() {
        assertFalse(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.EPL, "final"));
        assertFalse(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.CL, "1/4"));
        assertFalse(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.CL, "1/8 [1]"));
        assertFalse(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.CL, "1/16 [2]"));
        assertFalse(KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(League.LeagueCode.WC, "1/4"));
    }
}
