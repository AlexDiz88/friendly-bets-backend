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
                        .primaryProvider(MatchDataProviders.FOOTBALL_DATA)
                        .secondaryProvider(MatchDataProviders.API_FOOTBALL)
                        .dualVerificationEnabled(false)
                        .requireStablePolls(2)
                        .minMinutesAfterKickoff(105)
                        .minMinutesAfterKickoffKnockout(150)
                        .minMinutesSinceApiLastUpdated(30)
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
    void dualModeRequiresMatchingScores() {
        when(settingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOOTBALL_DATA)
                        .secondaryProvider(MatchDataProviders.API_FOOTBALL)
                        .dualVerificationEnabled(true)
                        .allowFinalizeWithoutSecondary(false)
                        .requireStablePolls(2)
                        .minMinutesAfterKickoff(105)
                        .minMinutesAfterKickoffKnockout(150)
                        .minMinutesSinceApiLastUpdated(30)
                        .build()
        );
        LocalDateTime now = LocalDateTime.now();
        GameScore score = GameScore.builder().fullTime("2:1").firstTime("1:0").build();
        GameResultRecord record = validRecord();
        record.setGameScore(score);
        record.setSources(Map.of(
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.API_FOOTBALL),
                GameResultSourceSnapshot.builder().gameScore(GameScore.builder().fullTime("1:0").firstTime("1:0").build()).build()
        ));
        when(stabilizationService.isStableEnough(record, now)).thenReturn(true);

        assertEquals(MatchResultTrustPolicy.FinalizeDecision.PROVIDER_MISMATCH, policy.evaluate(record, now));
    }

    private static GameResultRecord validRecord() {
        return GameResultRecord.builder()
                .gameScore(GameScore.builder().fullTime("2:1").firstTime("1:0").build())
                .utcDate(LocalDateTime.now().minusHours(3))
                .build();
    }
}
