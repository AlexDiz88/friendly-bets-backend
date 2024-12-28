package net.friendly_bets.services;

import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsPage;
import net.friendly_bets.dto.AllStatsByTeamsInSeasonDto;
import net.friendly_bets.dto.StatsByTeamsDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;
    @Mock
    private PlayerStatsByTeamsRepository playerStatsByTeamsRepository;
    @Mock
    private BetsRepository betsRepository;
    @Mock
    private PlayerStatsService playerStatsService;
    @Mock
    private TeamStatsService teamStatsService;
    @Mock
    private GameweekStatsService gameweekStatsService;
    @Mock
    private GetEntityService getEntityService;

    @InjectMocks
    private StatsService statsService;

    private final String seasonId = "seasonId";
    private final String leagueId1 = "leagueId1";
    private final String leagueId2 = "leagueId2";
    private Season season;
    private League league1;
    private League league2;
    private User user1;
    private User user2;
    private TeamStats teamStats1;
    private TeamStats teamStats2;
    private PlayerStats playerStats1;
    private PlayerStats playerStats2;

    @BeforeEach
    void setUp() {
        league1 = League.builder()
                .id(leagueId1)
                .leagueCode(League.LeagueCode.EPL)
                .build();

        league2 = League.builder()
                .id(leagueId2)
                .leagueCode(League.LeagueCode.BL)
                .build();

        user1 = User.builder().id("userId1").build();
        user2 = User.builder().id("userId2").build();

        season = Season.builder()
                .id(seasonId)
                .leagues(new ArrayList<>(List.of(league1, league2)))
                .players(new ArrayList<>(List.of(user1, user2)))
                .build();

        teamStats1 = TeamStats.builder().team(Team.builder().id("teamId1").build()).build();
        teamStats2 = TeamStats.builder().team(Team.builder().id("teamId2").build()).build();

        playerStats1 = PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(leagueId1)
                .user(user1)
                .totalBets(5)
                .betCount(4)
                .wonBetCount(3)
                .returnedBetCount(2)
                .lostBetCount(1)
                .emptyBetCount(0)
                .sumOfOdds(14.0)
                .sumOfWonOdds(8.5)
                .actualBalance(20.0)
                .build();

        playerStats2 = PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(leagueId2)
                .user(user2)
                .totalBets(10)
                .betCount(8)
                .wonBetCount(6)
                .returnedBetCount(4)
                .lostBetCount(2)
                .emptyBetCount(1)
                .sumOfOdds(19.0)
                .sumOfWonOdds(15.0)
                .actualBalance(35.0)
                .build();
    }

    @Test
    @DisplayName("Should return AllPlayersStatsPage when valid seasonId is provided and players stats exist")
    void getAllPlayersStatsBySeason_ReturnsAllPlayersStatsPage_WhenPlayerStatsExist() {
        // given
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user1)).thenReturn(playerStats1);
        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user2)).thenReturn(playerStats2);

        // when
        AllPlayersStatsPage result = statsService.getAllPlayersStatsBySeason(seasonId);

        // then
        assertNotNull(result);
        assertEquals(2, result.getPlayersStats().size());
        assertEquals(playerStats1.getTotalBets(), result.getPlayersStats().get(0).getTotalBets());
        assertEquals(playerStats2.getTotalBets(), result.getPlayersStats().get(1).getTotalBets());
        verify(getEntityService, times(1)).getSeasonOrThrow(seasonId);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user1);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user2);
    }

    @Test
    @DisplayName("Should return AllPlayersStatsPage when one player has stats and another player not")
    void getAllPlayersStatsBySeason_ReturnsAllPlayersStatsPage_WhenOnePlayerHasStatsAndAnotherNot() {
        // given
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user1)).thenReturn(playerStats1);
        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user2)).thenReturn(null);

        // when
        AllPlayersStatsPage result = statsService.getAllPlayersStatsBySeason(seasonId);

        // then
        assertNotNull(result);
        assertEquals(1, result.getPlayersStats().size());
        assertEquals(playerStats1.getTotalBets(), result.getPlayersStats().get(0).getTotalBets());
        verify(getEntityService, times(1)).getSeasonOrThrow(seasonId);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user1);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user2);
    }

    @Test
    @DisplayName("Should return empty AllPlayersStatsPage when no players stats exist")
    void getAllPlayersStatsBySeason_ReturnsEmptyPage_WhenNoPlayerStatsExist() {
        // given
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(getEntityService.getPlayerStatsOrNull(anyString(), anyString(), any())).thenReturn(null);

        // when
        AllPlayersStatsPage result = statsService.getAllPlayersStatsBySeason(seasonId);

        // then
        assertNotNull(result);
        assertTrue(result.getPlayersStats().isEmpty());
        verify(getEntityService, times(1)).getSeasonOrThrow(seasonId);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user1);
        verify(getEntityService, times(1)).getPlayerStatsOrNull(seasonId, TOTAL_ID, user2);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return AllPlayersStatsByLeaguesDto with valid data when stats by leagues exist")
    void getAllPlayersStatsByLeagues_ReturnsStatsByLeagues_WhenPlayerStatsExist() {
        // given
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(playerStatsRepository.findAllBySeasonId(seasonId)).thenReturn(List.of(playerStats1, playerStats2));

        // when
        AllPlayersStatsByLeaguesDto result = statsService.getAllPlayersStatsByLeagues(seasonId);

        // then
        assertNotNull(result);
        assertEquals(2, result.getPlayersStatsByLeagues().size());
        verify(playerStatsRepository, times(1)).findAllBySeasonId(seasonId);
    }

    @Test
    @DisplayName("Should return empty AllPlayersStatsByLeaguesDto when no stats by leagues exist")
    void getAllPlayersStatsByLeagues_ReturnsEmpty_WhenNoPlayerStatsExist() {
        // given
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(playerStatsRepository.findAllBySeasonId(seasonId)).thenReturn(Collections.emptyList());

        // when
        AllPlayersStatsByLeaguesDto result = statsService.getAllPlayersStatsByLeagues(seasonId);

        // then
        assertNotNull(result);
        assertTrue(result.getPlayersStatsByLeagues().isEmpty());
        verify(playerStatsRepository, times(1)).findAllBySeasonId(seasonId);
    }

    @Test
    @DisplayName("Should correctly group stats by leagueId and filter out players with zero bets")
    void getAllPlayersStatsByLeagues_GroupsStatsByLeague_WhenPlayerStatsExistAndFilterOutPlayersWithNoBets() {
        // given
        PlayerStats playerStats3 = PlayerStats.builder()
                .leagueId(leagueId2)
                .totalBets(3)
                .user(user1)
                .build();

        PlayerStats playerStatsWithoutBets = PlayerStats.builder()
                .leagueId(leagueId1)
                .totalBets(0)
                .user(new User())
                .build();

        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(playerStatsRepository.findAllBySeasonId(seasonId))
                .thenReturn(List.of(playerStats1, playerStats2, playerStats3, playerStatsWithoutBets));

        // when
        AllPlayersStatsByLeaguesDto result = statsService.getAllPlayersStatsByLeagues(seasonId);

        // then
        assertNotNull(result);
        assertEquals(2, result.getPlayersStatsByLeagues().size());
        assertEquals(1, result.getPlayersStatsByLeagues().get(0).getPlayersStats().size()); // 1 player in leagueId1
        assertEquals(2, result.getPlayersStatsByLeagues().get(1).getPlayersStats().size()); // 2 players in leagueId2
    }

    @Test
    @DisplayName("Should exclude leagues without players or with zero total bets")
    void getAllPlayersStatsByLeagues_ExcludesEmptyLeagues() {
        // given
        League league3 = League.builder()
                .id("leagueId3")
                .leagueCode(League.LeagueCode.CL)
                .build();

        PlayerStats playerStats3 = PlayerStats.builder()
                .leagueId("leagueId3")
                .totalBets(0)
                .user(user1)
                .build();

        season.setLeagues(List.of(league1, league2, league3));
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);
        when(playerStatsRepository.findAllBySeasonId(seasonId)).thenReturn(List.of(playerStats1, playerStats2, playerStats3));

        // when
        AllPlayersStatsByLeaguesDto result = statsService.getAllPlayersStatsByLeagues(seasonId);

        // then
        assertNotNull(result);
        assertEquals(2, result.getPlayersStatsByLeagues().size()); // player stats by league3 should be filtered out
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return AllStatsByTeamsInSeasonDto when stats for teams exist")
    void getAllStatsByTeamsInSeason_ReturnsAllStatsByTeamsInSeasonDto_WhenStatsExist() {
        // given
        PlayerStatsByTeams statsByTeams1 = PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .userId(user1.getId())
                .teamStats(List.of(teamStats1))
                .build();
        PlayerStatsByTeams statsByTeams2 = PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .userId(user2.getId())
                .teamStats(List.of(teamStats1, teamStats2))
                .build();

        List<PlayerStatsByTeams> statsList = List.of(statsByTeams1, statsByTeams2);
        when(playerStatsByTeamsRepository.findAllBySeasonId(seasonId)).thenReturn(Optional.of(statsList));

        // when
        AllStatsByTeamsInSeasonDto result = statsService.getAllStatsByTeamsInSeason(seasonId);

        // then
        assertNotNull(result);
        assertEquals(2, result.getPlayersStatsByTeams().size());
        assertEquals(1, result.getPlayersStatsByTeams().get(0).getTeamStats().size());
        assertEquals(2, result.getPlayersStatsByTeams().get(1).getTeamStats().size());
        verify(playerStatsByTeamsRepository, times(1)).findAllBySeasonId(seasonId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when no stats for teams are found")
    void getAllStatsByTeamsInSeason_ThrowsNotFoundException_WhenNoStatsExist() {
        // given
        when(playerStatsByTeamsRepository.findAllBySeasonId(seasonId)).thenReturn(Optional.empty());

        // when + then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> statsService.getAllStatsByTeamsInSeason(seasonId));

        assertEquals(NotFoundException.formatMessage("Season", seasonId), exception.getMessage());
        verify(playerStatsByTeamsRepository, times(1)).findAllBySeasonId(seasonId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return StatsByTeamsDto when valid seasonId, leagueId, and userId are provided")
    void getStatsByTeams_ReturnsStatsByTeamsDto_WhenStatsExist() {
        // given
        String userId = user1.getId();
        PlayerStatsByTeams playerStatsByTeams = PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .leagueId(leagueId1)
                .userId(userId)
                .teamStats(List.of(teamStats1, teamStats2))
                .build();

        when(getEntityService.getPlayerStatsByTeamsOrThrow(seasonId, leagueId1, userId)).thenReturn(playerStatsByTeams);

        // when
        StatsByTeamsDto result = statsService.getStatsByTeams(seasonId, leagueId1, userId);

        // then
        assertNotNull(result);
        assertEquals(playerStatsByTeams.getSeasonId(), result.getStatsByTeams().getSeasonId());
        assertEquals(playerStatsByTeams.getLeagueId(), result.getStatsByTeams().getLeagueId());
        assertEquals(playerStatsByTeams.getUserId(), result.getStatsByTeams().getUserId());
        assertEquals(2, result.getStatsByTeams().getTeamStats().size());
        verify(getEntityService, times(1)).getPlayerStatsByTeamsOrThrow(seasonId, leagueId1, userId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should correctly delete previous data, recalculate player stats and return AllPlayersStatsPage")
    void playersStatsFullRecalculation_RecalculatesStatsAndReturnsAllPlayersStatsPage() {
        // given
        Bet bet1 = Bet.builder()
                .season(season)
                .user(user1)
                .league(league1)
                .betStatus(Bet.BetStatus.OPENED)
                .betOdds(1.8)
                .betSize(10)
                .build();

        Bet bet2 = Bet.builder()
                .season(season)
                .user(user1)
                .league(league1)
                .betStatus(Bet.BetStatus.WON)
                .betOdds(2.5)
                .betSize(10)
                .balanceChange(15.0)
                .build();

        Bet bet3 = Bet.builder()
                .season(season)
                .user(user1)
                .league(league2)
                .betStatus(Bet.BetStatus.EMPTY)
                .betSize(10)
                .balanceChange(-10.0)
                .build();

        Bet bet4 = Bet.builder()
                .season(season)
                .user(user2)
                .league(league1)
                .betStatus(Bet.BetStatus.RETURNED)
                .betOdds(2.0)
                .betSize(10)
                .balanceChange(0.0)
                .build();

        Bet bet5 = Bet.builder()
                .season(season)
                .user(user2)
                .league(league2)
                .betStatus(Bet.BetStatus.LOST)
                .betOdds(3.5)
                .betSize(10)
                .balanceChange(-10.0)
                .build();

        Bet bet6 = Bet.builder()
                .season(season)
                .user(user2)
                .league(league2)
                .betStatus(Bet.BetStatus.DELETED)
                .build();

        List<Bet> bets = List.of(bet1, bet2, bet3, bet4, bet5, bet6);

        PlayerStats totalPlayerStats1 = PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(TOTAL_ID)
                .user(user1)
                .totalBets(1)
                .betCount(0)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .emptyBetCount(0)
                .sumOfOdds(0.0)
                .sumOfWonOdds(0.0)
                .actualBalance(0.0)
                .build();

        PlayerStats totalPlayerStats2 = PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(TOTAL_ID)
                .user(user2)
                .totalBets(1)
                .betCount(0)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .emptyBetCount(0)
                .sumOfOdds(0.0)
                .sumOfWonOdds(0.0)
                .actualBalance(0.0)
                .build();

        when(betsRepository.findAllBySeason_Id(seasonId)).thenReturn(bets);
        when(getEntityService.getSeasonOrThrow(seasonId)).thenReturn(season);

        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user1)).thenReturn(totalPlayerStats1);
        when(getEntityService.getPlayerStatsOrNull(seasonId, TOTAL_ID, user2)).thenReturn(totalPlayerStats2);

        when(playerStatsService.createNewStats(seasonId, leagueId1, user1)).thenReturn(playerStats1);
        when(playerStatsService.createNewStats(seasonId, leagueId2, user1)).thenReturn(playerStats1);
        when(playerStatsService.createNewStats(seasonId, TOTAL_ID, user1)).thenReturn(totalPlayerStats1);

        when(playerStatsService.createNewStats(seasonId, leagueId1, user2)).thenReturn(playerStats2);
        when(playerStatsService.createNewStats(seasonId, leagueId2, user2)).thenReturn(playerStats2);
        when(playerStatsService.createNewStats(seasonId, TOTAL_ID, user2)).thenReturn(totalPlayerStats2);

        // when
        AllPlayersStatsPage result = statsService.playersStatsFullRecalculation(seasonId);

        // then
        System.out.println(result);
        assertNotNull(result);
        assertEquals(2, result.getPlayersStats().size());
        assertEquals(2, result.getPlayersStats().get(0).getBetCount());
        assertEquals(1, result.getPlayersStats().get(0).getWonBetCount());
        assertEquals(0, result.getPlayersStats().get(0).getReturnedBetCount());
        assertEquals(0, result.getPlayersStats().get(0).getLostBetCount());
        assertEquals(1, result.getPlayersStats().get(0).getEmptyBetCount());
        assertEquals(100.0, result.getPlayersStats().get(0).getWinRate());
        assertEquals(2.5, result.getPlayersStats().get(0).getAverageOdds());
        assertEquals(2.5, result.getPlayersStats().get(0).getAverageWonBetOdds());
        assertEquals(5.0, result.getPlayersStats().get(0).getActualBalance());

        assertEquals(2, result.getPlayersStats().get(1).getBetCount());
        assertEquals(0, result.getPlayersStats().get(1).getWonBetCount());
        assertEquals(1, result.getPlayersStats().get(1).getReturnedBetCount());
        assertEquals(1, result.getPlayersStats().get(1).getLostBetCount());
        assertEquals(0, result.getPlayersStats().get(1).getEmptyBetCount());
        assertEquals(0.0, result.getPlayersStats().get(1).getWinRate());
        assertEquals(2.75, result.getPlayersStats().get(1).getAverageOdds());
        assertEquals(0.0, result.getPlayersStats().get(1).getAverageWonBetOdds());
        assertEquals(-10.0, result.getPlayersStats().get(1).getActualBalance());


        verify(playerStatsRepository, times(1)).deleteAllBySeasonId(seasonId);
        verify(betsRepository, times(1)).findAllBySeason_Id(seasonId);
        verify(playerStatsRepository, times(2)).save(playerStats1);
        verify(playerStatsRepository, times(1)).save(totalPlayerStats1);
        verify(playerStatsRepository, times(2)).save(playerStats2);
        verify(playerStatsRepository, times(1)).save(totalPlayerStats2);
    }

    // ------------------------------------------------------------------------------------------------------ //


}
