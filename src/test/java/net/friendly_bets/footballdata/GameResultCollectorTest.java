package net.friendly_bets.footballdata;

import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.*;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultCollectorTest {

    @Mock
    BetsRepository betsRepository;
    @Mock
    GameResultRecordRepository gameResultRecordRepository;
    @Mock
    FootballDataMatchdaySupport matchdaySupport;
    @Mock
    GetEntityService getEntityService;
    @Mock
    TeamAliasResolver teamAliasResolver;
    @Mock
    MatchResultSyncSettingsService syncSettingsService;
    @Mock
    GameResultsSyncRepository gameResultsSyncRepository;
    @InjectMocks
    GameResultCollector collector;

    @Test
    @DisplayName("collectForSeason uses only finalized game results")
    void collectForSeason_requiresFinalized() {
        when(syncSettingsService.getEffective()).thenReturn(
                MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings.builder()
                        .autoSettleOnlyWhenMatchdayCompleted(false)
                        .build()
        );
        Season season = Season.builder().id("s1").startDate(LocalDate.of(2025, 8, 1)).build();
        League league = League.builder().id("l1").leagueCode(League.LeagueCode.EPL).build();
        Team home = Team.builder().id("h1").title("Brighton").build();
        Team away = Team.builder().id("a1").title("ManchesterUnited").build();
        Bet bet = Bet.builder()
                .league(League.builder().id("l1").build())
                .homeTeam(Team.builder().id("h1").build())
                .awayTeam(Team.builder().id("a1").build())
                .matchDay("37")
                .betStatus(Bet.BetStatus.OPENED)
                .build();

        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .home(GameResultSideSnapshot.builder().externalId("397").externalName("Brighton").build())
                .away(GameResultSideSnapshot.builder().externalId("66").externalName("Man City").build())
                .build();
        GameResultRecord finalized = GameResultRecord.builder()
                .leagueCode("EPL")
                .leagueId("l1")
                .status("FINISHED")
                .homeTeamId("h1")
                .awayTeamId("a1")
                .gameScore(GameScore.builder().fullTime("1:2").firstTime("0:1").build())
                .finalizedAt(LocalDateTime.now())
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), source))
                .build();

        when(betsRepository.findAllBySeason_IdAndBetStatus("s1", Bet.BetStatus.OPENED)).thenReturn(List.of(bet));
        when(getEntityService.getLeagueOrThrow("l1")).thenReturn(league);
        when(getEntityService.getTeamOrThrow("h1")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("a1")).thenReturn(away);
        when(matchdaySupport.resolveFootballDataSeasonYear(season)).thenReturn("2025");
        when(matchdaySupport.resolveSlotOrder(league, "37")).thenReturn(Optional.of(37));
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason("EPL", 37, "2025"))
                .thenReturn(List.of(finalized));

        assertEquals(1, collector.collectForSeason(season).size());
    }
}
