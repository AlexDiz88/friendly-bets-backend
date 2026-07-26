package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwentyFourScoreLiveSupportTest {

    private static final Instant NOW = Instant.parse("2026-07-27T12:00:00Z");

    @Test
    void httpCandidate_atKickoffAndInPlay_notBeforeOrAfterFinished() {
        assertFalse(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(60)).status("SCHEDULED").build(), NOW));
        assertTrue(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW).status("SCHEDULED").build(), NOW));
        assertTrue(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.minusSeconds(600)).status("LIVE").build(), NOW));
        assertFalse(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(NOW.minusSeconds(600)).status("FINISHED").build(), NOW));
        assertFalse(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder()
                        .utcKickoff(NOW.minusSeconds(600))
                        .status("LIVE")
                        .fullDetailsFetchedAt(NOW)
                        .build(),
                NOW));
    }

    @Test
    void httpCandidate_stopsAfterLiveWindowUnlessStillInPlay() {
        Instant oldKickoff = NOW.minusSeconds(TwentyFourScoreLiveSupport.LIVE_WINDOW_SECONDS + 1);
        assertFalse(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(oldKickoff).status("SCHEDULED").build(), NOW));
        assertTrue(TwentyFourScoreLiveSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(oldKickoff).status("LIVE").build(), NOW));
    }

    @Test
    void needsFull_onlyFinishedWithoutFullDetails() {
        assertTrue(TwentyFourScoreLiveSupport.needsFullMatch(
                MatchSchedule.builder().status("FINISHED").build()));
        assertFalse(TwentyFourScoreLiveSupport.needsFullMatch(
                MatchSchedule.builder().status("FINISHED").fullDetailsFetchedAt(NOW).build()));
        assertFalse(TwentyFourScoreLiveSupport.needsFullMatch(
                MatchSchedule.builder().status("LIVE").build()));
    }

    @Test
    void upcomingKickoff_onlyFutureNotStarted() {
        assertEquals(NOW.plusSeconds(120), TwentyFourScoreLiveSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(120)).status("SCHEDULED").build(), NOW));
        assertNull(TwentyFourScoreLiveSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW).status("SCHEDULED").build(), NOW));
        assertNull(TwentyFourScoreLiveSupport.upcomingKickoffOrNull(
                MatchSchedule.builder().utcKickoff(NOW.plusSeconds(120)).status("LIVE").build(), NOW));
    }
}
