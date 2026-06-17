package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultFinalizerTest {

    @Mock
    MatchResultTrustPolicy trustPolicy;

    @Mock
    ApiSyncIssueService apiSyncIssueService;

    @Mock
    MatchResultSyncSettingsService settingsService;

    GameResultFinalizer finalizer;

    @BeforeEach
    void setUp() {
        finalizer = new GameResultFinalizer(trustPolicy, apiSyncIssueService, settingsService);
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .requireStablePolls(2)
                        .dualVerificationEnabled(true)
                        .minMinutesAfterKickoff(0)
                        .build()
        );
    }

    @Test
    @DisplayName("Does not finalize IN_PLAY even with full score")
    void doesNotFinalizeInPlay() {
        GameResultRecord record = finishedLikeRecord("IN_PLAY", "1:1", "0:1");
        finalizer.tryFinalize(record, LocalDateTime.now());
        assertNull(record.getFinalizedAt());
        verify(trustPolicy, never()).evaluate(any(), any());
    }

    @Test
    @DisplayName("Does not log invalid score before poll cycle threshold")
    void doesNotLogInvalidScoreBeforePollCycleThreshold() {
        GameResultRecord record = GameResultRecord.builder()
                .status("FINISHED")
                .pollCycleCount(1)
                .gameScore(GameScore.builder()
                        .fullTime("2:2")
                        .firstTime("1:0")
                        .overTime("0:0")
                        .penalty("3:3")
                        .build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .stableScorePollCount(2)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        GameResultSourceSnapshot.builder()
                                .apiLastUpdated(LocalDateTime.now().minusHours(2))
                                .build()))
                .build();

        when(trustPolicy.evaluate(any(), any())).thenReturn(MatchResultTrustPolicy.FinalizeDecision.INVALID_SCORE);

        finalizer.tryFinalize(record, LocalDateTime.now());

        assertNull(record.getFinalizedAt());
        verify(apiSyncIssueService, never()).recordInvalidCanonicalScore(any());
    }

    @Test
    @DisplayName("Logs invalid score after poll cycle threshold")
    void logsInvalidScoreAfterPollCycleThreshold() {
        GameResultRecord record = GameResultRecord.builder()
                .status("FINISHED")
                .pollCycleCount(2)
                .gameScore(GameScore.builder()
                        .fullTime("2:2")
                        .firstTime("1:0")
                        .overTime("0:0")
                        .penalty("3:3")
                        .build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .stableScorePollCount(2)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        GameResultSourceSnapshot.builder()
                                .apiLastUpdated(LocalDateTime.now().minusHours(2))
                                .build()))
                .build();

        when(trustPolicy.evaluate(any(), any())).thenReturn(MatchResultTrustPolicy.FinalizeDecision.INVALID_SCORE);

        finalizer.tryFinalize(record, LocalDateTime.now());

        assertNull(record.getFinalizedAt());
        verify(apiSyncIssueService).recordInvalidCanonicalScore(record);
    }

    @Test
    @DisplayName("Finalizes when trust policy READY")
    void finalizesWhenReady() {
        LocalDateTime now = LocalDateTime.now();
        GameResultRecord record = finishedLikeRecord("FINISHED", "2:1", "1:0");
        when(trustPolicy.evaluate(record, now)).thenReturn(MatchResultTrustPolicy.FinalizeDecision.READY);

        finalizer.tryFinalize(record, now);

        assertNotNull(record.getFinalizedAt());
    }

    private static GameResultRecord finishedLikeRecord(String status, String fullTime, String firstTime) {
        return GameResultRecord.builder()
                .status(status)
                .gameScore(GameScore.builder().fullTime(fullTime).firstTime(firstTime).build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .build();
    }
}
