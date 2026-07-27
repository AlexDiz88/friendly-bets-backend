package net.friendly_bets.sportsru;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SportsRuCalendarPathSupportTest {

    @Test
    void resolvesSlugToCalendarPath() {
        assertEquals(
                "/football/tournament/premier-league/calendar/",
                SportsRuCalendarPathSupport.resolveCalendarPath("premier-league")
        );
        assertEquals(
                "/football/tournament/bundesliga/calendar/",
                SportsRuCalendarPathSupport.resolveCalendarPath(" bundesliga ")
        );
    }

    @Test
    void keepsFullPath() {
        assertEquals(
                "/football/tournament/ucl/calendar/",
                SportsRuCalendarPathSupport.resolveCalendarPath("/football/tournament/ucl/calendar")
        );
    }

    @Test
    void rejectsInvalid() {
        assertNull(SportsRuCalendarPathSupport.resolveCalendarPath(""));
        assertNull(SportsRuCalendarPathSupport.resolveCalendarPath("foo/bar"));
        assertNull(SportsRuCalendarPathSupport.resolveCalendarPath("../x"));
    }
}
