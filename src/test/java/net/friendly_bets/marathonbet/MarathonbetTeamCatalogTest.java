package net.friendly_bets.marathonbet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetTeamCatalogTest {

    @Test
    void resolvesMarathonRussianNames() {
        assertEquals("MEX", MarathonbetTeamCatalog.fifaCodeForMarathonName("Мексика").orElseThrow());
        assertEquals("KOR", MarathonbetTeamCatalog.fifaCodeForMarathonName("Республика Корея").orElseThrow());
        assertEquals("BIH", MarathonbetTeamCatalog.fifaCodeForMarathonName("Босния и Герцеговина").orElseThrow());
    }
}
