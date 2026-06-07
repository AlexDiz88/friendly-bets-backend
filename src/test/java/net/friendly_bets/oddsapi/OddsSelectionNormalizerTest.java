package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsSelectionNormalizerTest {

    private final OddsMatchContext mexicoSa = OddsMatchContext.of("Mexico", "South Africa");

    @Test
    void doubleChance_shorthand() {
        assertEquals(
                Optional.of(OddsSelectionCode.DC_1X),
                OddsSelectionNormalizer.normalize(OddsMarketCategory.DOUBLE_CHANCE, "1X", mexicoSa)
        );
    }

    @Test
    void doubleChance_bet365Labels() {
        assertEquals(
                Optional.of(OddsSelectionCode.DC_1X),
                OddsSelectionNormalizer.normalize(OddsMarketCategory.DOUBLE_CHANCE, "mexico or draw", mexicoSa)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_12),
                OddsSelectionNormalizer.normalize(OddsMarketCategory.DOUBLE_CHANCE, "mexico or south africa", mexicoSa)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_X2),
                OddsSelectionNormalizer.normalize(OddsMarketCategory.DOUBLE_CHANCE, "draw or south africa", mexicoSa)
        );
    }

    @Test
    void doubleChance_apiTeamNames() {
        OddsMatchContext koreaCzech = OddsMatchContext.of("Korea Republic", "Czechia");
        assertEquals(
                Optional.of(OddsSelectionCode.DC_1X),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "south korea or draw", koreaCzech)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_X2),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "draw or czechia", koreaCzech)
        );
        OddsMatchContext turkeyAus = OddsMatchContext.of("Türkiye", "Australia");
        assertEquals(
                Optional.of(OddsSelectionCode.DC_12),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "australia or türkiye", turkeyAus)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_12),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "south korea or czechia", koreaCzech)
        );
        OddsMatchContext usaParaguay = OddsMatchContext.of("USA", "Paraguay");
        assertEquals(
                Optional.of(OddsSelectionCode.DC_1X),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "usa or draw", usaParaguay)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_12),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "usa or paraguay", usaParaguay)
        );
        OddsMatchContext ivoryEcuador = OddsMatchContext.of("Côte d'Ivoire", "Ecuador");
        assertEquals(
                Optional.of(OddsSelectionCode.DC_1X),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "ivory coast or draw", ivoryEcuador)
        );
        assertEquals(
                Optional.of(OddsSelectionCode.DC_12),
                OddsSelectionNormalizer.normalize(
                        OddsMarketCategory.DOUBLE_CHANCE, "ivory coast or ecuador", ivoryEcuador)
        );
    }

    @Test
    void displayLabels() {
        assertEquals("1X", OddsSelectionCode.DC_1X.displayLabel());
        assertEquals("П1", OddsSelectionCode.HOME.displayLabel());
    }
}
