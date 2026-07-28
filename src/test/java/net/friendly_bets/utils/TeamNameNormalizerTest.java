package net.friendly_bets.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamNameNormalizerTest {

    @Test
    @DisplayName("case and yo/e are normalized for matching")
    void normalizesCaseAndYo() {
        assertEquals("мельде", TeamNameNormalizer.normalize("Мёльде"));
        assertEquals("мельде", TeamNameNormalizer.normalize("Мельде"));
        assertTrue(TeamNameNormalizer.equalsNormalized("РБ Лейпциг", "Рб Лейпциг"));
        assertTrue(TeamNameNormalizer.equalsNormalized("  Arsenal ", "arsenal"));
    }
}
