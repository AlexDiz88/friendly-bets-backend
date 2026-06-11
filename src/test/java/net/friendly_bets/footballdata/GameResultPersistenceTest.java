package net.friendly_bets.footballdata;

import net.friendly_bets.gameresults.GameResultFinalizer;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultStabilizationService;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultFinalizedSource;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultPersistenceTest {

    @Mock
    ApiSyncIssueService apiSyncIssueService;

    @Mock
    GameResultFinalizer gameResultFinalizer;

    @Mock
    MatchResultStabilizationService stabilizationService;

    @Mock
    MatchResultSyncSettingsService matchResultSyncSettingsService;

    @InjectMocks
    GameResultPersistence persistence;

    @org.junit.jupiter.api.BeforeEach
    void setUpSettings() {
        lenient().when(matchResultSyncSettingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .primaryProvider(MatchDataProviders.FOOTBALL_DATA)
                        .secondaryProvider(MatchDataProviders.API_FOOTBALL)
                        .dualVerificationEnabled(false)
                        .build()
        );
    }

    @Test
    void applyProvisionalSync_updatesScoreWithoutForcingFinalizeOnInPlay() {
        LocalDateTime first = LocalDateTime.of(2026, 5, 28, 10, 0);
        GameResultRecord existing = GameResultRecord.builder()
                .status("IN_PLAY")
                .gameScore(GameScore.builder().fullTime("1:0").firstTime("1:0").build())
                .fetchedAt(first)
                .build();

        GameResultRecord incoming = GameResultRecord.builder()
                .status("IN_PLAY")
                .gameScore(GameScore.builder().fullTime("1:1").firstTime("1:0").build())
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                        GameResultSourceSnapshot.builder().status("IN_PLAY").build()
                ))
                .build();

        persistence.applyProvisionalSync(existing, incoming, first.plusHours(2));

        verify(apiSyncIssueService).recordApiScoreChangedIfNeeded(existing, incoming);
        verify(stabilizationService).updateStabilityCounters(existing, first.plusHours(2));
        verify(gameResultFinalizer).tryFinalize(existing, first.plusHours(2));
        assertEquals("IN_PLAY", existing.getStatus());
        assertEquals("1:1", existing.getGameScore().getFullTime());
    }

    @Test
    void applyFinalizedApiSync_doesNotOverwriteScore() {
        LocalDateTime first = LocalDateTime.of(2026, 5, 28, 10, 0);
        LocalDateTime second = LocalDateTime.of(2026, 5, 28, 12, 0);
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("2:1").build())
                .apiLastUpdated(first)
                .fetchedAt(first)
                .build();
        Map<String, GameResultSourceSnapshot> sources = new HashMap<>();
        sources.put(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), source);
        GameResultRecord existing = GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("2:1").build())
                .fetchedAt(first)
                .finalizedAt(first)
                .finalizedSource(GameResultFinalizedSource.API.name())
                .sources(sources)
                .build();

        GameResultRecord incoming = GameResultRecord.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("9:9").build())
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                        GameResultSourceSnapshot.builder()
                                .status("FINISHED")
                                .gameScore(GameScore.builder().fullTime("9:9").build())
                                .apiLastUpdated(second)
                                .build()
                ))
                .build();

        persistence.applyFinalizedApiSync(existing, incoming, second);

        verify(apiSyncIssueService).recordApiScoreChangedIfNeeded(existing, incoming);
        assertEquals("2:1", existing.getGameScore().getFullTime());
        assertEquals(second, existing.getFetchedAt());
        assertEquals("9:9", existing.footballDataSource().getGameScore().getFullTime());
        assertEquals(second, existing.footballDataSource().getApiLastUpdated());
    }

    @Test
    void isLockedAgainstApiSync_whenAdminCorrected() {
        GameResultRecord record = GameResultRecord.builder().adminCorrected(true).build();
        assertTrue(persistence.isLockedAgainstApiSync(record));
        verify(apiSyncIssueService, never()).recordApiScoreChangedIfNeeded(any(), any());
    }
}
