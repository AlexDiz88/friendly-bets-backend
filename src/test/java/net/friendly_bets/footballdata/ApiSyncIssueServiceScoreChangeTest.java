package net.friendly_bets.footballdata;

import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiSyncIssueServiceScoreChangeTest {

    @Mock
    ApiSyncIssueRepository apiSyncIssueRepository;

    @Mock
    TeamAliasResolver teamAliasResolver;

    @InjectMocks
    ApiSyncIssueService apiSyncIssueService;

    @Test
    @DisplayName("recordApiScoreChangedIfNeeded creates issue when api lastUpdated is newer and score differs")
    void recordApiScoreChangedIfNeeded_createsIssue() {
        LocalDateTime storedUpdated = LocalDateTime.of(2026, 5, 28, 11, 25);
        LocalDateTime incomingUpdated = LocalDateTime.of(2026, 5, 30, 0, 20);
        GameResultSourceSnapshot storedSource = GameResultSourceSnapshot.builder()
                .externalMatchId(538161L)
                .apiLastUpdated(storedUpdated)
                .gameScore(GameScore.builder().fullTime("2:2").firstTime("1:0").build())
                .home(GameResultSideSnapshot.builder().externalName("Manchester City FC").build())
                .away(GameResultSideSnapshot.builder().externalName("Aston Villa FC").build())
                .build();
        GameResultRecord existing = GameResultRecord.builder()
                .id("gr-1")
                .leagueCode("EPL")
                .season("2025")
                .matchday(38)
                .gameScore(GameScore.builder().fullTime("2:2").firstTime("1:0").build())
                .finalizedAt(LocalDateTime.of(2026, 5, 30, 10, 42))
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), storedSource))
                .build();
        GameResultSourceSnapshot incomingSource = GameResultSourceSnapshot.builder()
                .externalMatchId(538161L)
                .apiLastUpdated(incomingUpdated)
                .gameScore(GameScore.builder().fullTime("1:2").firstTime("1:0").build())
                .home(GameResultSideSnapshot.builder().externalName("Manchester City FC").build())
                .away(GameResultSideSnapshot.builder().externalName("Aston Villa FC").build())
                .build();
        GameResultRecord incoming = GameResultRecord.builder()
                .gameScore(GameScore.builder().fullTime("1:2").firstTime("1:0").build())
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), incomingSource))
                .build();

        when(apiSyncIssueRepository.findFirstByProviderAndIssueTypeAndGameResultId(
                MatchDataProviders.FOOTBALL_DATA,
                ApiSyncIssue.IssueType.API_SCORE_CHANGED.name(),
                "gr-1"
        )).thenReturn(Optional.empty());

        apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);

        ArgumentCaptor<ApiSyncIssue> captor = ArgumentCaptor.forClass(ApiSyncIssue.class);
        verify(apiSyncIssueRepository).save(captor.capture());
        ApiSyncIssue saved = captor.getValue();
        assertEquals(ApiSyncIssue.IssueType.API_SCORE_CHANGED.name(), saved.getIssueType());
        assertEquals("gr-1", saved.getGameResultId());
        assertEquals(538161L, saved.getExternalMatchId());
        assertTrue(saved.getMessage().contains("stored=2:2 (1:0)"));
        assertTrue(saved.getMessage().contains("api=1:2 (1:0)"));
        assertTrue(saved.getMessage().contains("finalized"));
    }

    @Test
    @DisplayName("recordApiScoreChangedIfNeeded skips when api lastUpdated is not newer")
    void recordApiScoreChangedIfNeeded_skipsWhenNotNewer() {
        LocalDateTime sameUpdated = LocalDateTime.of(2026, 5, 28, 11, 25);
        GameResultSourceSnapshot storedSource = GameResultSourceSnapshot.builder()
                .apiLastUpdated(sameUpdated)
                .build();
        GameResultRecord existing = GameResultRecord.builder()
                .id("gr-1")
                .gameScore(GameScore.builder().fullTime("2:2").firstTime("1:0").build())
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), storedSource))
                .build();
        GameResultSourceSnapshot incomingSource = GameResultSourceSnapshot.builder()
                .apiLastUpdated(sameUpdated)
                .gameScore(GameScore.builder().fullTime("1:2").firstTime("1:0").build())
                .build();
        GameResultRecord incoming = GameResultRecord.builder()
                .gameScore(GameScore.builder().fullTime("1:2").firstTime("1:0").build())
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), incomingSource))
                .build();

        apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);

        verify(apiSyncIssueRepository, never()).save(any());
    }
}
