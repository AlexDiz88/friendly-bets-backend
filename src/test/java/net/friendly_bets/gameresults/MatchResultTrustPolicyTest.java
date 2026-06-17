package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultTrustPolicyTest {

    @Mock
    MatchResultSyncSettingsService settingsService;

    @Mock
    MatchResultStabilizationService stabilizationService;

    MatchResultTrustPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MatchResultTrustPolicy(settingsService, stabilizationService);
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
                        .allowFinalizeWithoutSecondary(false)
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
        LocalDateTime kickoff = LocalDateTime.of(2026, 6, 14, 18, 0);
        LocalDateTime now = kickoff.plusMinutes(90);
        GameResultRecord record = validRecord();
        record.setUtcDate(kickoff);

        assertEquals(MatchResultTrustPolicy.FinalizeDecision.TOO_EARLY, policy.evaluate(record, now));
    }

    private static GameResultRecord validRecord() {
        return GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("2:1").firstTime("1:0").build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .build();
    }
}
