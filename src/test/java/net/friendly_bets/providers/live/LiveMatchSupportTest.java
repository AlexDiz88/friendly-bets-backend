package net.friendly_bets.providers.live;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveMatchSupportTest {

    private static final Instant NOW = Instant.parse("2026-07-27T12:00:00Z");

    @Test
    void httpCandidate_requiresUtcKickoffEvenWhenInPlay() {
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().status("LIVE").build(), NOW));
        assertFalse(LiveMatchSupport.isMissingUtcKickoffSkip(
                MatchSchedule.builder().status("FINISHED").build()));
        assertTrue(LiveMatchSupport.isMissingUtcKickoffSkip(
                MatchSchedule.builder().status("SCHEDULED").build()));
    }

    @Test
    void httpCandidate_atKickoffAndInPlay_notBeforeOrAfterFinished() {
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(60)).status("SCHEDULED").build(), NOW));
        assertTrue(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW).status("SCHEDULED").build(), NOW));
        assertTrue(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.minusSeconds(600)).status("LIVE").build(), NOW));
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.minusSeconds(600)).status("FINISHED").build(), NOW));
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder()
                        .utcKickoff(NOW.minusSeconds(600))
                        .status("LIVE")
                        .fullDetailsFetchedAt(NOW)
                        .build(),
                NOW));
    }

    @Test
    void httpCandidate_keepsPollingExtraTimeAndStopsOnCanceled() {
        Instant oldKickoff = NOW.minusSeconds(LiveMatchSupport.LIVE_WINDOW_SECONDS + 1);
        assertTrue(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(oldKickoff).status("EXTRA_TIME").build(), NOW));
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.minusSeconds(600)).status("CANCELED").build(), NOW));
    }

    @Test
    void httpCandidate_stopsAfterLiveWindowUnlessStillInPlay() {
        Instant oldKickoff = NOW.minusSeconds(LiveMatchSupport.LIVE_WINDOW_SECONDS + 1);
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(oldKickoff).status("SCHEDULED").build(), NOW));
        assertTrue(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(oldKickoff).status("LIVE").build(), NOW));
    }

    @Test
    void needsFull_onlyFinishedWithoutFullDetails() {
        assertTrue(LiveMatchSupport.needsFullMatch(
                MatchSchedule.builder().status("FINISHED").build()));
        assertFalse(LiveMatchSupport.needsFullMatch(
                MatchSchedule.builder().status("FINISHED").fullDetailsFetchedAt(NOW).build()));
        assertFalse(LiveMatchSupport.needsFullMatch(
                MatchSchedule.builder().status("LIVE").build()));
        assertFalse(LiveMatchSupport.needsFullMatch(
                MatchSchedule.builder().status("CANCELED").build()));
    }

    @Test
    void upcomingKickoff_onlyFutureNotStarted() {
        assertEquals(NOW.plusSeconds(120), LiveMatchSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(120)).status("SCHEDULED").build(), NOW));
        assertNull(LiveMatchSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW).status("SCHEDULED").build(), NOW));
        assertNull(LiveMatchSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(120)).status("LIVE").build(), NOW));
    }
}
