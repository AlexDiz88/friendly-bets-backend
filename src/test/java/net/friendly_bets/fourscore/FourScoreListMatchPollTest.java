package net.friendly_bets.fourscore;

import net.friendly_bets.models.gameresults.GameResultRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourScoreListMatchPollTest {

    @Test
    void shouldPollForLiveMatchNotFinalized() {
        FourScoreListMatch live = FourScoreListMatch.builder()
                .statusText("Идёт 72'")
                .homeScore(0)
                .awayScore(1)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .status("IN_PLAY")
                .build();
        assertTrue(live.shouldPollForRecord(record));
    }

    @Test
    void shouldNotPollForFinalizedRecord() {
        FourScoreListMatch live = FourScoreListMatch.builder()
                .statusText("Идёт 72'")
                .homeScore(0)
                .awayScore(1)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .finalizedAt(LocalDateTime.now())
                .build();
        assertFalse(live.shouldPollForRecord(record));
    }

    @Test
    void shouldNotPollForNotStarted() {
        FourScoreListMatch scheduled = FourScoreListMatch.builder()
                .statusText("Не началось")
                .homeScore(0)
                .awayScore(0)
                .build();
        assertFalse(scheduled.shouldPollForRecord(null));
    }

    @Test
    void shouldPollForLiveListStatusLiveAtZeroZero() {
        FourScoreListMatch live = FourScoreListMatch.builder()
                .statusText("Идёт 38'")
                .homeScore(0)
                .awayScore(0)
                .build();
        assertTrue(live.shouldPollForRecord(GameResultRecord.builder().status("SCHEDULED").build()));
    }

    @Test
    void shouldPollAfterKickoffWhenListStatusMissing() {
        FourScoreListMatch listMatch = FourScoreListMatch.builder()
                .homeScore(0)
                .awayScore(0)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .status("SCHEDULED")
                .utcDate(LocalDateTime.now().minusHours(2))
                .build();
        assertTrue(listMatch.shouldPollForRecord(record));
    }

    @Test
    void shouldNotPollBeforeKickoffWhenListStatusMissing() {
        FourScoreListMatch listMatch = FourScoreListMatch.builder()
                .homeScore(0)
                .awayScore(0)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .status("SCHEDULED")
                .utcDate(LocalDateTime.now().plusHours(2))
                .build();
        assertFalse(listMatch.shouldPollForRecord(record));
    }
}
