package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStatsByTeams;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamStats;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static net.friendly_bets.utils.StatsUtils.*;


@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class TeamStatsService {

    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;


    public void calculateStatsByTeams(String seasonId, String leagueId, String userId, Bet bet, boolean isPlus) {
        processStatsByTeams(seasonId, leagueId, userId, bet, isPlus);
        processStatsByTeams(seasonId, leagueId, TOTAL_ID, bet, isPlus);
    }

    public void processStatsByTeams(String seasonId, String leagueId, String userId, Bet bet, boolean isPlus) {
        PlayerStatsByTeams playerStatsByTeams = getStatsByTeamsOrCreateNew(seasonId, leagueId, userId);
        processTeamsStats(playerStatsByTeams, bet, isPlus);
        if (!playerStatsByTeams.getTeamStats().isEmpty()) {
            saveStatsByTeams(playerStatsByTeams);
        }
    }

    public void processTeamsStats(PlayerStatsByTeams playerStatsByTeams, Bet bet, boolean isPlus) {
        for (Team team : Arrays.asList(bet.getHomeTeam(), bet.getAwayTeam())) {
            TeamStats teamStats = getTeamStatsOrCreateNew(playerStatsByTeams, team);
            modifyTeamStats(teamStats, bet, isPlus);
            if (!isPlus) {
                removeTeamIfNoBets(playerStatsByTeams, teamStats);
                removeIfNoStats(playerStatsByTeams);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void modifyTeamStats(TeamStats teamStats, Bet bet, boolean isPlus) {
        updateBetCount(teamStats, isPlus);
        updateBetCountValuesBasedOnBetStatus(teamStats, bet.getBetStatus(), bet.getBetOdds(), isPlus);
        updateSumOfOddsAndActualBalance(teamStats, bet.getBetOdds(), bet.getBalanceChange(), isPlus);
        recalculateStats(teamStats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void removeTeamIfNoBets(PlayerStatsByTeams playerStatsByTeams, TeamStats teamStats) {
        if (teamStats.getBetCount() == 0) {
            playerStatsByTeams.getTeamStats().remove(teamStats);
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void removeIfNoStats(PlayerStatsByTeams stats) {
        if (stats.getTeamStats().isEmpty()) {
            playerStatsByTeamsRepository.deleteBySeasonIdAndLeagueIdAndUserId(stats.getSeasonId(), stats.getLeagueId(), stats.getUserId());
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void saveStatsByTeams(PlayerStatsByTeams playerStatsByTeams) {
        playerStatsByTeamsRepository.save(playerStatsByTeams);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStatsByTeams getStatsByTeamsOrCreateNew(String seasonId, String leagueId, String userId) {
        return playerStatsByTeamsRepository
                .findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId)
                .orElseGet(() -> createNewStatsByTeams(seasonId, leagueId, userId));
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStatsByTeams createNewStatsByTeams(String seasonId, String leagueId, String userId) {
        return PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .userId(userId)
                .teamStats(new ArrayList<>())
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    private TeamStats getTeamStatsOrCreateNew(PlayerStatsByTeams playerStatsByTeams, Team team) {
        return playerStatsByTeams.getTeamStats().stream()
                .filter(ts -> ts.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(() -> {
                    TeamStats newTeamStats = createNewTeamStats(team);
                    playerStatsByTeams.getTeamStats().add(newTeamStats);
                    return newTeamStats;
                });
    }

    // ------------------------------------------------------------------------------------------------------ //

    private TeamStats createNewTeamStats(Team team) {
        return TeamStats.builder()
                .team(team)
                .betCount(0)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .emptyBetCount(0)
                .winRate(0.0)
                .averageOdds(0.0)
                .averageWonBetOdds(0.0)
                .actualBalance(0.0)
                .sumOfOdds(0.0)
                .sumOfWonOdds(0.0)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

}
