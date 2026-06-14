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
    void dualVerificationOffIgnoresSecondaryApiLastUpdated() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(false, 1));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusHours(2));
        record.setProvider(MatchDataProviders.FOURSCORE);
        record.setSources(Map.of(
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                GameResultSourceSnapshot.builder()
                        .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                        .build(),
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                GameResultSourceSnapshot.builder()
                        .apiLastUpdated(fetchedAt.minusSeconds(30))
                        .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                        .build()
        ));

        assertTrue(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void dualVerificationOnUsesPrimaryApiLastUpdatedNotSecondary() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(true, 5));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusHours(2));
        record.setProvider(MatchDataProviders.FOURSCORE);
        record.setSources(Map.of(
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                GameResultSourceSnapshot.builder()
                        .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                        .build(),
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                GameResultSourceSnapshot.builder()
                        .apiLastUpdated(fetchedAt.minusSeconds(30))
                        .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                        .build()
        ));

        assertTrue(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void dualVerificationOnBlocksWhenPrimaryApiLastUpdatedTooRecent() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(true, 5));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusHours(2));
        record.setProvider(MatchDataProviders.FOOTBALL_DATA);
        record.setSources(Map.of(
                MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                GameResultSourceSnapshot.builder()
                        .apiLastUpdated(fetchedAt.minusMinutes(2))
                        .gameScore(GameScore.builder().fullTime("7:1").firstTime("3:1").build())
                        .build()
        ));

        assertFalse(service.isStableEnough(record, fetchedAt));
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
            boolean dualVerificationEnabled,
            int minMinutesSinceApiLastUpdated
    ) {
        return MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                .primaryProvider(MatchDataProviders.FOURSCORE)
                .secondaryProvider(MatchDataProviders.FOOTBALL_DATA)
                .dualVerificationEnabled(dualVerificationEnabled)
                .requireStablePolls(2)
                .minMinutesAfterKickoff(105)
                .minMinutesAfterKickoffKnockout(150)
                .minMinutesSinceApiLastUpdated(minMinutesSinceApiLastUpdated)
                .build();
    }
}
