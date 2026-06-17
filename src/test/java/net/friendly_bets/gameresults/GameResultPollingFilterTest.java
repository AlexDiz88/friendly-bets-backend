package net.friendly_bets.gameresults;

import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GameResultPollingFilterTest {

    private GameResultPollingFilter filter;

    @BeforeEach
    void setUp() {
        Wc26ScheduleMatchRepository scheduleRepository = mock(Wc26ScheduleMatchRepository.class);
        filter = new GameResultPollingFilter(new GameResultEffectiveKickoff(scheduleRepository));
    }

    @Test
    void needsPollForLiveStatus() {
        GameResultRecord record = GameResultRecord.builder()
                .status("IN_PLAY")
                .build();
        assertTrue(filter.needsExternalPoll(record));
    }

    @Test
    void needsPollAfterKickoffWhenScheduled() {
        GameResultRecord record = GameResultRecord.builder()
                .status("SCHEDULED")
                .utcDate(LocalDateTime.now().minusMinutes(30))
                .build();
        assertTrue(filter.needsExternalPoll(record));
    }

    @Test
    void skipsFinalized() {
        GameResultRecord record = GameResultRecord.builder()
                .status("IN_PLAY")
                .finalizedAt(LocalDateTime.now())
                .build();
        assertFalse(filter.needsExternalPoll(record));
    }

    @Test
    void skipsNotStartedBeforeKickoff() {
        GameResultRecord record = GameResultRecord.builder()
                .status("SCHEDULED")
                .utcDate(LocalDateTime.now().plusHours(2))
                .build();
        assertFalse(filter.needsExternalPoll(record));
    }

    @Test
    void needsPollWhenLiveMinutePresent() {
        GameResultRecord record = GameResultRecord.builder()
                .status("SCHEDULED")
                .liveMinuteLabel("48'")
                .utcDate(LocalDateTime.now().plusHours(2))
                .build();
        assertTrue(filter.needsExternalPoll(record));
    }
}
