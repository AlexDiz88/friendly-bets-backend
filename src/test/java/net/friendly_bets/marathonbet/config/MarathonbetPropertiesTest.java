package net.friendly_bets.marathonbet.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetPropertiesTest {

    @Test
    void scheduleHoursForLeague_parsesCommaSeparated() {
        MarathonbetProperties props = new MarathonbetProperties();
        props.setLeagueScheduleHours(Map.of("EPL", "0,12", "BL", "3,15"));
        assertEquals(java.util.List.of(0, 12), props.scheduleHoursForLeague("EPL"));
        assertTrue(props.isLeagueHourDue("BL", 3));
        assertFalse(props.isLeagueHourDue("BL", 0));
    }
}
