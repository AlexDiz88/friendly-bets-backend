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
    void httpCandidate_keepsScheduledPastFormerFourHourWindow() {
        Instant fiveHoursAgo = NOW.minusSeconds(5 * 3600L);
        assertTrue(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(fiveHoursAgo).status("SCHEDULED").build(), NOW),
                "stuck SCHEDULED after missed wake must stay pollable within max window");
    }

    @Test
    void httpCandidate_stopsAfterMaxPollWindowForAnyNonTerminal() {
        Instant veryOldKickoff = NOW.minusSeconds(LiveMatchSupport.LIVE_IN_PLAY_MAX_POLL_SECONDS + 1);
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(veryOldKickoff).status("SCHEDULED").build(), NOW));
        assertFalse(LiveMatchSupport.isLiveHttpCandidate(
                MatchSchedule.builder().utcKickoff(veryOldKickoff).status("IN_PLAY").build(), NOW));
    }

    @Test
    void secondaryCatchUp_onlyAfterExpectedDurationWhileStillNonTerminal() {
        Instant recent = NOW.minusSeconds(90 * 60L);
        assertFalse(LiveMatchSupport.needsSecondaryStatusCatchUp(
                MatchSchedule.builder().utcKickoff(recent).status("IN_PLAY").build(), NOW));
        Instant overdue = NOW.minusSeconds(LiveMatchSupport.SECONDARY_CATCHUP_AFTER_KICKOFF_SECONDS + 60);
        assertTrue(LiveMatchSupport.needsSecondaryStatusCatchUp(
                MatchSchedule.builder().utcKickoff(overdue).status("IN_PLAY").build(), NOW));
        assertFalse(LiveMatchSupport.needsSecondaryStatusCatchUp(
                MatchSchedule.builder().utcKickoff(overdue).status("FINISHED").build(), NOW));
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
