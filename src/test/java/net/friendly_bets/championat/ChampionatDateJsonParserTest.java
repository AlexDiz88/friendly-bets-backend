package net.friendly_bets.championat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChampionatDateJsonParserTest {

    @Test
    void mapStatus_usesExplicitLabels() {
        assertEquals("IN_PLAY", ChampionatDateJsonParser.mapStatus("1t"));
        assertEquals("IN_PLAY", ChampionatDateJsonParser.mapStatus("2t"));
        assertEquals("PAUSED", ChampionatDateJsonParser.mapStatus("half"));
        assertEquals("EXTRA_TIME", ChampionatDateJsonParser.mapStatus("extra"));
        assertEquals("PENALTY_SHOOTOUT", ChampionatDateJsonParser.mapStatus("pen"));
        assertEquals("FINISHED", ChampionatDateJsonParser.mapStatus("fin"));
        assertEquals("SCHEDULED", ChampionatDateJsonParser.mapStatus("dns"));
        assertEquals("CANCELED", ChampionatDateJsonParser.mapStatus("cans"));
    }

    @Test
    void extractMinute_fromStatusName() {
        assertEquals("90+1", ChampionatDateJsonParser.extractMinuteLabel("2t", "2-й тайм, 90+1'"));
        assertEquals("75", ChampionatDateJsonParser.extractMinuteLabel("2t", "2-й тайм, 75'"));
        assertEquals("77", ChampionatDateJsonParser.extractMinuteLabel("2t", "2-й тайм, 77'"));
        assertEquals("38", ChampionatDateJsonParser.extractMinuteLabel("1t", "1-й тайм, 38'"));
        assertEquals("98", ChampionatDateJsonParser.extractMinuteLabel("extra", "доп. время, 98'"));
        assertEquals("120", ChampionatDateJsonParser.extractMinuteLabel("extra", "доп. время, 120'"));
        assertNull(ChampionatDateJsonParser.extractMinuteLabel("half", "перерыв"));
        assertNull(ChampionatDateJsonParser.extractMinuteLabel("fin", "окончен"));
    }

    @Test
    void formatShootout_fromScoreNode() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var withShootout = mapper.readTree("{\"totalHome\":0,\"totalAway\":0,\"shootoutHome\":2,\"shootoutAway\":3}");
        assertEquals("2:3", ChampionatDateJsonParser.formatShootoutScore(withShootout));
        var without = mapper.readTree("{\"totalHome\":1,\"totalAway\":0}");
        assertNull(ChampionatDateJsonParser.formatShootoutScore(without));
    }
}
