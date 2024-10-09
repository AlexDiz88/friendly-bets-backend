package net.friendly_bets.services;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStatsByTeams;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamStats;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamStatsServiceTest {

    @Mock
    private PlayerStatsByTeamsRepository playerStatsByTeamsRepo;

    @InjectMocks
    private TeamStatsService teamStatsService;

    private final String seasonId = "seasonId";
    private final String leagueId = "leagueId";
    private final String userId = "userId";
    private final String homeTeamId = "homeTeamId";
    private final String awayTeamId = "awayTeamId";

    private Bet bet;
    private TeamStats homeTeamUserStats;
    private TeamStats awayTeamUserStats;
    private TeamStats homeTeamTotalStats;
    private TeamStats awayTeamTotalStats;
    private Team homeTeam;
    private Team awayTeam;
    private PlayerStatsByTeams userStats;
    private PlayerStatsByTeams totalStats;

    @BeforeEach
    void setUp() {
        homeTeam = Team.builder().id(homeTeamId).build();
        awayTeam = Team.builder().id(awayTeamId).build();

        bet = Bet.builder()
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .betStatus(Bet.BetStatus.WON)
                .betOdds(5.0)
                .betSize(10)
                .balanceChange(40.0)
                .build();

        homeTeamUserStats = createTeamStats(homeTeamId, 6, 2, 2, 2, 12.0, 4.0, 10.0);
        awayTeamUserStats = createTeamStats(awayTeamId, 9, 3, 3, 3, 18.0, 6.0, 20.0);
        userStats = createPlayerStatsByTeams(userId, new ArrayList<>(List.of(homeTeamUserStats, awayTeamUserStats)));

        homeTeamTotalStats = createTeamStats(homeTeamId, 12, 4, 4, 4, 24.0, 8.0, 30.0);
        awayTeamTotalStats = createTeamStats(awayTeamId, 15, 5, 5, 5, 30.0, 10.0, 40.0);
        totalStats = createPlayerStatsByTeams(TOTAL_ID, new ArrayList<>(List.of(homeTeamTotalStats, awayTeamTotalStats)));
    }

    private TeamStats createTeamStats(String teamId, int betCount, int wonCount, int returnedCount, int lostCount, double odds, double wonOdds, double balance) {
        return TeamStats.builder()
                .team(Team.builder().id(teamId).build())
                .betCount(betCount)
                .wonBetCount(wonCount)
                .returnedBetCount(returnedCount)
                .lostBetCount(lostCount)
                .emptyBetCount(0)
                .sumOfOdds(odds)
                .sumOfWonOdds(wonOdds)
                .actualBalance(balance)
                .build();
    }

    private PlayerStatsByTeams createPlayerStatsByTeams(String userId, List<TeamStats> teamStatsList) {
        return PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .userId(userId)
                .teamStats(new ArrayList<>(teamStatsList))
                .build();
    }

    @Test
    @DisplayName("Should correctly call methods and not create new player stats or team stats when they already exist")
    void calculateStatsByTeams_ShouldCallMethodsCorrectly_WhenPlayerStatsAndTotalStatsAndTeamsAlreadyExist() {
        TeamStatsService spyService = spy(teamStatsService);
        // given


        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.of(userStats));
        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, TOTAL_ID))
                .thenReturn(Optional.of(totalStats));

        // when
        spyService.calculateStatsByTeams(seasonId, leagueId, userId, bet, true);

        // then
        assertEquals(2, userStats.getTeamStats().size());
        assertEquals(2, totalStats.getTeamStats().size());

        verify(playerStatsByTeamsRepo, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
        verify(playerStatsByTeamsRepo, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, TOTAL_ID);
        verify(playerStatsByTeamsRepo, times(1)).save(userStats);
        verify(playerStatsByTeamsRepo, times(1)).save(totalStats);
        verify(spyService, never()).createNewStatsByTeams(seasonId, leagueId, userId);
        verify(spyService, never()).createNewStatsByTeams(seasonId, leagueId, TOTAL_ID);
        verify(spyService, never()).createNewTeamStats(homeTeam);
        verify(spyService, never()).createNewTeamStats(awayTeam);
    }

    @Test
    @DisplayName("Should create new stats when player stats or team stats are missing")
    void calculateStatsByTeams_ShouldCreateNewStats_WhenPlayerOrTeamStatsMissing() {
        TeamStatsService spyService = spy(teamStatsService);
        // given
        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.empty());
        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, TOTAL_ID))
                .thenReturn(Optional.empty());

        // when
        spyService.calculateStatsByTeams(seasonId, leagueId, userId, bet, true);

        // then
        verify(playerStatsByTeamsRepo, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
        verify(playerStatsByTeamsRepo, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, TOTAL_ID);
        verify(playerStatsByTeamsRepo, times(2)).save(any(PlayerStatsByTeams.class));
        verify(spyService, times(1)).createNewStatsByTeams(seasonId, leagueId, userId);
        verify(spyService, times(1)).createNewStatsByTeams(seasonId, leagueId, TOTAL_ID);
        verify(spyService, times(2)).createNewTeamStats(homeTeam);
        verify(spyService, times(2)).createNewTeamStats(awayTeam);
    }

    static Stream<Arguments> provideStats() {
        return Stream.of(
                arguments(true, Bet.BetStatus.WON, 40.0,
                        7, 10, 13, 16, // expectedBetCount
                        3, 4, 5, 6, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        17.0, 23.0, 29.0, 35.0, // expectedSumOfOdds
                        9.0, 11.0, 13.0, 15.0, // expectedSumOfWonOdds
                        50.0, 60.0, 70.0, 80.0 // expectedActualBalance
                ),
                arguments(false, Bet.BetStatus.WON, 40.0,
                        5, 8, 11, 14, // expectedBetCount
                        1, 2, 3, 4, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        7.0, 13.0, 19.0, 25.0, // expectedSumOfOdds
                        -1.0, 1.0, 3.0, 5.0, // expectedSumOfWonOdds
                        -30.0, -20.0, -10.0, 0.0 // expectedActualBalance
                ),
                arguments(true, Bet.BetStatus.RETURNED, 0.0,
                        7, 10, 13, 16, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        3, 4, 5, 6, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        17.0, 23.0, 29.0, 35.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                ),
                arguments(false, Bet.BetStatus.RETURNED, 0.0,
                        5, 8, 11, 14, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        1, 2, 3, 4, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        7.0, 13.0, 19.0, 25.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                ),
                arguments(true, Bet.BetStatus.LOST, -10.0,
                        7, 10, 13, 16, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        3, 4, 5, 6, // expectedLostBetCount
                        17.0, 23.0, 29.0, 35.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        0.0, 10.0, 20.0, 30.0 // expectedActualBalance
                ),
                arguments(false, Bet.BetStatus.LOST, -10.0,
                        5, 8, 11, 14, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        1, 2, 3, 4, // expectedLostBetCount
                        7.0, 13.0, 19.0, 25.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        20.0, 30.0, 40.0, 50.0 // expectedActualBalance
                ),
                arguments(true, Bet.BetStatus.OPENED, null,
                        7, 10, 13, 16, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        12.0, 18.0, 24.0, 30.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                ),
                arguments(false, Bet.BetStatus.OPENED, null,
                        5, 8, 11, 14, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        12.0, 18.0, 24.0, 30.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                ),
                arguments(true, Bet.BetStatus.EMPTY, null,
                        7, 10, 13, 16, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        12.0, 18.0, 24.0, 30.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                ),
                arguments(false, Bet.BetStatus.EMPTY, null,
                        5, 8, 11, 14, // expectedBetCount
                        2, 3, 4, 5, // expectedWonBetCount
                        2, 3, 4, 5, // expectedReturnedBetCount
                        2, 3, 4, 5, // expectedLostBetCount
                        12.0, 18.0, 24.0, 30.0, // expectedSumOfOdds
                        4.0, 6.0, 8.0, 10.0, // expectedSumOfWonOdds
                        10.0, 20.0, 30.0, 40.0 // expectedActualBalance
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideStats")
    @DisplayName("Should correctly update team stats when bet is processed")
    void calculateStatsByTeams_ShouldUpdateTeamStatsCorrectly(boolean isPlus, Bet.BetStatus betStatus, Double balanceChange,

                                                              int expectedUserHomeTeamBetCount, int expectedUserAwayTeamBetCount,
                                                              int expectedTotalHomeTeamBetCount, int expectedTotalAwayTeamBetCount,

                                                              int expectedUserHomeTeamWonBetCount, int expectedUserAwayTeamWonBetCount,
                                                              int expectedTotalHomeTeamWonBetCount, int expectedTotalAwayTeamWonBetCount,

                                                              int expectedUserHomeTeamReturnedBetCount, int expectedUserAwayTeamReturnedBetCount,
                                                              int expectedTotalHomeTeamReturnedBetCount, int expectedTotalAwayTeamReturnedBetCount,

                                                              int expectedUserHomeTeamLostBetCount, int expectedUserAwayTeamLostBetCount,
                                                              int expectedTotalHomeTeamLostBetCount, int expectedTotalAwayTeamLostBetCount,

                                                              double expectedUserHomeTeamSumOfOdds, double expectedUserAwayTeamSumOfOdds,
                                                              double expectedTotalHomeTeamSumOfOdds, double expectedTotalAwayTeamSumOfOdds,

                                                              double expectedUserHomeTeamSumOfWonOdds, double expectedUserAwayTeamSumOfWonOdds,
                                                              double expectedTotalHomeTeamSumOfWonOdds, double expectedTotalAwayTeamSumOfWonOdds,

                                                              double expectedUserHomeTeamActualBalance, double expectedUserAwayTeamActualBalance,
                                                              double expectedTotalHomeTeamActualBalance, double expectedTotalAwayTeamActualBalance) {
        // given
        bet.setBetStatus(betStatus);
        bet.setBetSize(10);
        bet.setBetOdds(5.0);
        bet.setBalanceChange(balanceChange);

        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.of(userStats));
        when(playerStatsByTeamsRepo.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, TOTAL_ID))
                .thenReturn(Optional.of(totalStats));

        // when
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, userId, bet, isPlus);

        // then
        assertEquals(expectedUserHomeTeamBetCount, homeTeamUserStats.getBetCount(), "UserHomeTeam bet count");
        assertEquals(expectedUserAwayTeamBetCount, awayTeamUserStats.getBetCount(), "UserAwayTeam bet count");
        assertEquals(expectedTotalHomeTeamBetCount, homeTeamTotalStats.getBetCount(), "TotalHomeTeam bet count");
        assertEquals(expectedTotalAwayTeamBetCount, awayTeamTotalStats.getBetCount(), "TotalAwayTeam bet count");

        assertEquals(expectedUserHomeTeamWonBetCount, homeTeamUserStats.getWonBetCount(), "UserHomeTeam won bet count");
        assertEquals(expectedUserAwayTeamWonBetCount, awayTeamUserStats.getWonBetCount(), "UserAwayTeam won bet count");
        assertEquals(expectedTotalHomeTeamWonBetCount, homeTeamTotalStats.getWonBetCount(), "TotalHomeTeam won bet count");
        assertEquals(expectedTotalAwayTeamWonBetCount, awayTeamTotalStats.getWonBetCount(), "TotalAwayTeam won bet count");

        assertEquals(expectedUserHomeTeamReturnedBetCount, homeTeamUserStats.getReturnedBetCount(), "UserHomeTeam returned bet count");
        assertEquals(expectedUserAwayTeamReturnedBetCount, awayTeamUserStats.getReturnedBetCount(), "UserAwayTeam returned bet count");
        assertEquals(expectedTotalHomeTeamReturnedBetCount, homeTeamTotalStats.getReturnedBetCount(), "TotalHomeTeam returned bet count");
        assertEquals(expectedTotalAwayTeamReturnedBetCount, awayTeamTotalStats.getReturnedBetCount(), "TotalAwayTeam returned bet count");

        assertEquals(expectedUserHomeTeamLostBetCount, homeTeamUserStats.getLostBetCount(), "UserHomeTeam lost bet count");
        assertEquals(expectedUserAwayTeamLostBetCount, awayTeamUserStats.getLostBetCount(), "UserAwayTeam lost bet count");
        assertEquals(expectedTotalHomeTeamLostBetCount, homeTeamTotalStats.getLostBetCount(), "TotalHomeTeam lost bet count");
        assertEquals(expectedTotalAwayTeamLostBetCount, awayTeamTotalStats.getLostBetCount(), "TotalAwayTeam lost bet count");

        assertEquals(expectedUserHomeTeamSumOfOdds, homeTeamUserStats.getSumOfOdds(), "UserHomeTeam sum of odds");
        assertEquals(expectedUserAwayTeamSumOfOdds, awayTeamUserStats.getSumOfOdds(), "UserAwayTeam sum of odds");
        assertEquals(expectedTotalHomeTeamSumOfOdds, homeTeamTotalStats.getSumOfOdds(), "TotalHomeTeam sum of odds");
        assertEquals(expectedTotalAwayTeamSumOfOdds, awayTeamTotalStats.getSumOfOdds(), "TotalAwayTeam sum of odds");

        assertEquals(expectedUserHomeTeamSumOfWonOdds, homeTeamUserStats.getSumOfWonOdds(), "UserHomeTeam sum of won odds");
        assertEquals(expectedUserAwayTeamSumOfWonOdds, awayTeamUserStats.getSumOfWonOdds(), "UserAwayTeam sum of won odds");
        assertEquals(expectedTotalHomeTeamSumOfWonOdds, homeTeamTotalStats.getSumOfWonOdds(), "TotalHomeTeam sum of won odds");
        assertEquals(expectedTotalAwayTeamSumOfWonOdds, awayTeamTotalStats.getSumOfWonOdds(), "TotalAwayTeam sum of won odds");

        assertEquals(expectedUserHomeTeamActualBalance, homeTeamUserStats.getActualBalance(), "UserHomeTeam actual balance");
        assertEquals(expectedUserAwayTeamActualBalance, awayTeamUserStats.getActualBalance(), "UserAwayTeam actual balance");
        assertEquals(expectedTotalHomeTeamActualBalance, homeTeamTotalStats.getActualBalance(), "TotalHomeTeam actual balance");
        assertEquals(expectedTotalAwayTeamActualBalance, awayTeamTotalStats.getActualBalance(), "TotalAwayTeam actual balance");
    }

}
