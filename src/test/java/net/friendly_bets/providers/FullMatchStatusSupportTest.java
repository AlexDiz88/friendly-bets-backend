package net.friendly_bets.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullMatchStatusSupportTest {

    @Test
    void finishedLabels() {
        assertTrue(FullMatchStatusSupport.isProviderFinished("Завершен"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("завершён"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("Завершён"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("Завершен. 7:6 по пенальти"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("Finished"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("FINISHED"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("FT"));
        assertTrue(FullMatchStatusSupport.isProviderFinished("AET"));
    }

    @Test
    void unfinishedOrUnknown_notFinished() {
        assertFalse(FullMatchStatusSupport.isProviderFinished(null));
        assertFalse(FullMatchStatusSupport.isProviderFinished(""));
        assertFalse(FullMatchStatusSupport.isProviderFinished("LIVE"));
        assertFalse(FullMatchStatusSupport.isProviderFinished("PAUSED"));
        assertFalse(FullMatchStatusSupport.isProviderFinished("2-й тайм"));
        assertFalse(FullMatchStatusSupport.isProviderFinished("45'"));
        assertFalse(FullMatchStatusSupport.isProviderFinished("Перерыв"));
    }
}
