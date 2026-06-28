package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.wc26.Wc26ScheduleKickoffLookup;
import net.friendly_bets.wc26.Wc26ScheduleKickoffResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultTrustPolicyTest {

    @Mock
    MatchResultSyncSettingsService settingsService;

    @Mock
    MatchResultStabilizationService stabilizationService;

    GameResultEffectiveKickoff effectiveKickoff;
    MatchResultTrustPolicy policy;

    @BeforeEach
    void setUp() {
        effectiveKickoff = new GameResultEffectiveKickoff(mock(Wc26ScheduleKickoffResolver.class));
        policy = new MatchResultTrustPolicy(settingsService, stabilizationService, effectiveKickoff);
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOURSCORE)
                        .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                        .dualVerificationEnabled(false)
                        .requireStablePolls(2)
                        .minMinutesAfterKickoff(0)
                        .build()
        );
    }

    @Test
    void primaryOnlyModeReadyWhenStable() {
        LocalDateTime now = LocalDateTime.now();
        GameResultRecord record = validRecord();
        when(stabilizationService.isStableEnough(record, now)).thenReturn(true);

        assertEquals(MatchResultTrustPolicy.FinalizeDecision.READY, policy.evaluate(record, now));
    }

    @Test
    void dualModeRequiresMatchingScoresAndSecondaryStable() {
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOURSCORE)
                        .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                        .dualVerificationEnabled(true)
                        .requireStablePolls(2)
                        .minMinutesAfterKickoff(0)
                        .build()
        );
        LocalDateTime now = LocalDateTime.now();
        GameScore score = GameScore.builder().fullTime("2:1").firstTime("1:0").build();
        GameResultRecord record = validRecord();
        record.setGameScore(score);
        record.setStableScorePollCount(2);
        record.setSources(Map.of(
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                GameResultSourceSnapshot.builder()
                        .status("FINISHED")
                        .gameScore(GameScore.builder().fullTime("1:0").firstTime("1:0").build())
                        .stableScorePollCount(2)
                        .build()
        ));
        when(stabilizationService.isStableEnough(record, now)).thenReturn(true);
        when(stabilizationService.isSecondaryStableEnough(record, MatchDataProviders.TWENTYFOUR_SCORE))
                .thenReturn(true);

        assertEquals(MatchResultTrustPolicy.FinalizeDecision.PROVIDER_MISMATCH, policy.evaluate(record, now));
    }

    @Test
    void blocksFinalizeBeforeMinMinutesAfterKickoff() {
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOURSCORE)
                        .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                        .dualVerificationEnabled(false)
                        .requireStablePolls(1)
                        .minMinutesAfterKickoff(105)
                        .build()
        );
        LocalDateTime kickoff = GameResultNotStarted.nowUtc().minusMinutes(90);
        GameResultRecord record = validRecord();
        record.setUtcDate(kickoff);

        assertEquals(
                MatchResultTrustPolicy.FinalizeDecision.TOO_EARLY,
                policy.evaluate(record, GameResultNotStarted.nowUtc())
        );
    }

    @Test
    void wc26UsesScheduleKickoffNotMislabeledFourScoreUtcDate() {
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOURSCORE)
                        .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                        .dualVerificationEnabled(false)
                        .requireStablePolls(1)
                        .minMinutesAfterKickoff(100)
                        .build()
        );
        LocalDateTime scheduleKickoffUtc = GameResultNotStarted.nowUtc().minusHours(3);
        Wc26ScheduleKickoffLookup.install(Map.of(23, scheduleKickoffUtc));
        GameResultRecord record = validRecord();
        record.setWc26ScheduleId(23);
        // 4score хранит локальное время страницы как utcDate — на 2ч «впереди» реального UTC.
        record.setUtcDate(scheduleKickoffUtc.plusHours(2));
        when(stabilizationService.isStableEnough(record, GameResultNotStarted.nowUtc())).thenReturn(true);

        assertEquals(MatchResultTrustPolicy.FinalizeDecision.READY, policy.evaluate(record, GameResultNotStarted.nowUtc()));
    }

    private static GameResultRecord validRecord() {
        return GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("2:1").firstTime("1:0").build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .build();
    }
}
