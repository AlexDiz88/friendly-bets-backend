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
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2, true));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusMinutes(102));

        assertTrue(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void notStableWhenPollCountBelowRequirement() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2, true));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 21, 0);
        GameResultRecord record = stableFinishedRecord(fetchedAt.minusHours(2));
        record.setStableScorePollCount(1);

        assertFalse(service.isStableEnough(record, fetchedAt));
    }

    @Test
    void secondaryStableWhenSourcePollCountMeetsRequirement() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2, true));
        GameResultRecord record = GameResultRecord.builder()
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        GameResultSourceSnapshot.builder().stableScorePollCount(2).build()
                ))
                .build();

        assertTrue(service.isSecondaryStableEnough(record, MatchDataProviders.TWENTYFOUR_SCORE));
    }

    @Test
    void dualModeIncrementsBothCountersTogether() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2, true));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 22, 0);
        GameScore score = GameScore.builder().fullTime("0:2").firstTime("0:1").build();
        GameResultSourceSnapshot secondary = GameResultSourceSnapshot.builder()
                .status("FINISHED")
                .gameScore(score)
                .scoreDuration(CanonicalScoreNormalizer.DURATION_REGULAR)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(score)
                .scoreDuration(CanonicalScoreNormalizer.DURATION_REGULAR)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        secondary
                ))
                .build();

        service.updateAfterPollCycle(record, fetchedAt);
        assertEquals(1, record.getStableScorePollCount());
        assertEquals(1, secondary.getStableScorePollCount());

        service.updateAfterPollCycle(record, fetchedAt);
        assertEquals(2, record.getStableScorePollCount());
        assertEquals(2, secondary.getStableScorePollCount());
    }

    @Test
    void dualModeResetsWhenScoresMismatch() {
        when(settingsService.getEffective()).thenReturn(effectiveSettings(2, true));
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 6, 14, 22, 0);
        GameResultSourceSnapshot secondary = GameResultSourceSnapshot.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("0:2").firstTime("0:0").build())
                .scoreDuration(CanonicalScoreNormalizer.DURATION_REGULAR)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("0:2").firstTime("0:1").build())
                .scoreDuration(CanonicalScoreNormalizer.DURATION_REGULAR)
                .stableScorePollCount(2)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        secondary
                ))
                .build();
        secondary.setStableScorePollCount(2);

        service.updateAfterPollCycle(record, fetchedAt);

        assertEquals(0, record.getStableScorePollCount());
        assertEquals(0, secondary.getStableScorePollCount());
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
            int requireStablePolls,
            boolean dualVerificationEnabled
    ) {
        return MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                .primaryProvider(MatchDataProviders.FOURSCORE)
                .secondaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                .dualVerificationEnabled(dualVerificationEnabled)
                .requireStablePolls(requireStablePolls)
                .minMinutesAfterKickoff(0)
                .build();
    }
}
