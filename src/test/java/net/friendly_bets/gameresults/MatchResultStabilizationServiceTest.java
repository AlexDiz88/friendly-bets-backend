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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultStabilizationServiceTest {

    @Mock
    MatchResultSyncSettingsService settingsService;

    MatchResultStabilizationService service;

    @BeforeEach
    void setUp() {
        service = new MatchResultStabilizationService(settingsService);
    }

    @Test
    void stableWhenPollCountMeetsRequirement() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusMinutes(102));

        assertTrue(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void notStableWhenPollCountBelowRequirement() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusHours(2));
        record.setStableScorePollCount(1);

        assertFalse(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void secondaryStableWhenSourcePollCountMeetsRequirement() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2));
        GameResultRecord record = GameResultRecord.builder()
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        GameResultSourceSnapshot.builder().stableScorePollCount(2).build()
                ))
                .build();

        assertTrue(service.isSecondaryStableEnough(record, MatchDataProviders.TWENTYFOUR_SCORE));
    }

    private static GameResultRecord stableFinishedRecord(LocalDateTime kickoff) {
        return GameResultRecord.builder()
                .status("FINISHED")
                .utcDate(kickoff)
                .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                .stableScorePollCount(8)
                .build();
    }

    private static MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings effectiveSettings(
            int requireStablePolls
    ) {
        return MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                .primaryProvider(MatchDataProviders.FOURSCORE)
                .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                .dualVerificationEnabled(true)
                .requireStablePolls(requireStablePolls)
                .minMinutesAfterKickoff(0)
                .minMinutesAfterKickoffKnockout(0)
                .minMinutesSinceApiLastUpdated(0)
                .build();
    }
}
