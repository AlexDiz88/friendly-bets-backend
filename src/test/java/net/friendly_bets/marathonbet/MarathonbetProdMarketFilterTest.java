package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.MarathonbetMarketDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetProdMarketFilterTest {

    @Test
    void ignoresMarketNameEndingWithThreeWaySuffix() {
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd("Победа с учетом форы (3 исхода)"));
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().name("  Победа с учетом форы (3 исхода)  ").build()
        ));
    }

    @Test
    void doesNotIgnoreTwoWayHandicap() {
        assertFalse(MarathonbetProdMarketFilter.isIgnoredForProd("Победа с учетом форы"));
        assertFalse(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().name("Канада (-1)").build()
        ));
    }

    @Test
    void ignoresAsianHandicapByMarketName() {
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd("Победа с учетом азиатской форы"));
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().model("MTCH_HB").name("Победа с учетом азиатской форы").build()
        ));
    }

    @Test
    void ignoresVoluntaryWinAndConsecutiveGoalsModels() {
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().model("MTCH_TEWFB").name("Волевая").build()
        ));
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().model("MTCH_T1NGI").name("2-й гол").build()
        ));
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().model("MTCH_T1GNM").name("Голы подряд").build()
        ));
        assertTrue(MarathonbetProdMarketFilter.isIgnoredForProd(
                MarathonbetMarketDto.builder().model("MTCH_HB_ASN").name("Фора").build()
        ));
    }
}
