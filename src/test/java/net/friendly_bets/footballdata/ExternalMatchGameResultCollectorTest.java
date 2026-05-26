package net.friendly_bets.footballdata;

import net.friendly_bets.models.*;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.ExternalMatchRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalMatchGameResultCollectorTest {

    @Mock
    BetsRepository betsRepository;
    @Mock
    ExternalMatchRepository externalMatchRepository;
    @Mock
    FootballDataMatchdaySupport matchdaySupport;
    @Mock
    GetEntityService getEntityService;
    @Mock
    TeamAliasResolver teamAliasResolver;

    @InjectMocks
    ExternalMatchGameResultCollector collector;

    @Test
    @DisplayName("collectForSeason returns GameResult for FINISHED external match with mapped teams")
    void collectForSeason_returnsSettleableResults() {
        Season season = Season.builder().id("s1").startDate(LocalDate.of(2025, 8, 1)).build();
        League league = League.builder().id("l1").leagueCode(League.LeagueCode.EPL).build();
        Team home = Team.builder().id("h1").build();
        Team away = Team.builder().id("a1").build();
        Bet bet = Bet.builder()
                .league(League.builder().id("l1").build())
                .homeTeam(Team.builder().id("h1").build())
                .awayTeam(Team.builder().id("a1").build())
                .matchDay("3")
                .betStatus(Bet.BetStatus.OPENED)
                .build();

        GameScore score = GameScore.builder().fullTime("2:1").firstTime("1:0").build();
        ExternalMatch match = ExternalMatch.builder()
                .status("FINISHED")
                .homeTeamId("h1")
                .awayTeamId("a1")
                .homeFootballDataTeamId(10)
                .awayFootballDataTeamId(20)
                .gameScore(score)
                .build();

        when(betsRepository.findAllBySeason_IdAndBetStatus("s1", Bet.BetStatus.OPENED)).thenReturn(List.of(bet));
        when(getEntityService.getLeagueOrThrow("l1")).thenReturn(league);
        when(getEntityService.getTeamOrThrow("h1")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("a1")).thenReturn(away);
        when(matchdaySupport.resolveFootballDataSeasonYear(season)).thenReturn("2025");
        when(matchdaySupport.resolveSlotOrder(league, "3")).thenReturn(Optional.of(3));
        when(externalMatchRepository.findByCompetitionCodeAndMatchdayAndSeason("PL", 3, "2025"))
                .thenReturn(List.of(match));

        var results = collector.collectForSeason(season);

        assertEquals(1, results.size());
        assertEquals("l1", results.get(0).getLeagueId());
        assertEquals("2:1", results.get(0).getGameScore().getFullTime());
    }

    @Test
    @DisplayName("collectForSeason matches cached FINISHED match via football-data team ids")
    void collectForSeason_matchesByFootballDataTeamIds() {
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

        ExternalMatch match = ExternalMatch.builder()
                .status("FINISHED")
                .homeFootballDataTeamId(397)
                .awayFootballDataTeamId(66)
                .homeTeamName("Brighton & Hove Albion FC")
                .awayTeamName("Manchester United FC")
                .gameScore(GameScore.builder().fullTime("1:2").build())
                .build();

        when(betsRepository.findAllBySeason_IdAndBetStatus("s1", Bet.BetStatus.OPENED)).thenReturn(List.of(bet));
        when(getEntityService.getLeagueOrThrow("l1")).thenReturn(league);
        when(getEntityService.getTeamOrThrow("h1")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("a1")).thenReturn(away);
        when(matchdaySupport.resolveFootballDataSeasonYear(season)).thenReturn("2025");
        when(matchdaySupport.resolveSlotOrder(league, "37")).thenReturn(Optional.of(37));
        when(externalMatchRepository.findByCompetitionCodeAndMatchdayAndSeason("PL", 37, "2025"))
                .thenReturn(List.of(match));
        when(teamAliasResolver.teamMatchesFootballDataSide(home, 397, "Brighton & Hove Albion FC"))
                .thenReturn(true);
        when(teamAliasResolver.teamMatchesFootballDataSide(away, 66, "Manchester United FC"))
                .thenReturn(true);

        var results = collector.collectForSeason(season);

        assertEquals(1, results.size());
        assertEquals("1:2", results.get(0).getGameScore().getFullTime());
    }

    @Test
    @DisplayName("collectForSeason skips non-terminal matches")
    void collectForSeason_skipsInPlay() {
        Season season = Season.builder().id("s1").startDate(LocalDate.of(2025, 8, 1)).build();
        League league = League.builder().id("l1").leagueCode(League.LeagueCode.EPL).build();
        Team home = Team.builder().id("h1").build();
        Team away = Team.builder().id("a1").build();
        Bet bet = Bet.builder()
                .league(League.builder().id("l1").build())
                .homeTeam(Team.builder().id("h1").build())
                .awayTeam(Team.builder().id("a1").build())
                .matchDay("1")
                .betStatus(Bet.BetStatus.OPENED)
                .build();

        when(betsRepository.findAllBySeason_IdAndBetStatus("s1", Bet.BetStatus.OPENED)).thenReturn(List.of(bet));
        when(getEntityService.getLeagueOrThrow("l1")).thenReturn(league);
        when(getEntityService.getTeamOrThrow("h1")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("a1")).thenReturn(away);
        when(matchdaySupport.resolveFootballDataSeasonYear(season)).thenReturn("2025");
        when(matchdaySupport.resolveSlotOrder(league, "1")).thenReturn(Optional.of(1));
        when(externalMatchRepository.findByCompetitionCodeAndMatchdayAndSeason("PL", 1, "2025"))
                .thenReturn(List.of(ExternalMatch.builder()
                        .status("IN_PLAY")
                        .homeTeamId("h1")
                        .awayTeamId("a1")
                        .homeFootballDataTeamId(1)
                        .awayFootballDataTeamId(2)
                        .gameScore(GameScore.builder().fullTime("0:0").build())
                        .build()));

        assertTrue(collector.collectForSeason(season).isEmpty());
    }
}
