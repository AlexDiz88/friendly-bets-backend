package net.friendly_bets.soccer365;

import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Soccer365ScheduleSyncSchedulerTest {

    @Test
    void isLeagueHourDue_spreadsLeaguesAcrossDay() {
        Soccer365Properties props = new Soccer365Properties();
        props.setLeagueHourBase(1);
        props.setLeagueHourStep(2);
        Soccer365ScheduleSyncScheduler scheduler = new Soccer365ScheduleSyncScheduler(
                props, null, null, null);

        assertTrue(scheduler.isLeagueHourDue(0, 1));
        assertTrue(scheduler.isLeagueHourDue(0, 13));
        assertFalse(scheduler.isLeagueHourDue(0, 2));

        assertTrue(scheduler.isLeagueHourDue(1, 3));
        assertTrue(scheduler.isLeagueHourDue(1, 15));
        assertFalse(scheduler.isLeagueHourDue(1, 1));
    }
}
