package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.PlayerStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static net.friendly_bets.utils.StatsUtils.*;


@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PlayerStatsService {

    PlayerStatsRepository playerStatsRepository;

    public void calculateStatsBasedOnNewOpenedBet(String seasonId, String leagueId, User user, boolean isPlus) {
        processStatsBasedOnNewOpenedBet(seasonId, leagueId, user, isPlus); // for league player stats
        processStatsBasedOnNewOpenedBet(seasonId, TOTAL_ID, user, isPlus); // for total player stats
    }

    public void processStatsBasedOnNewOpenedBet(String seasonId, String leagueId, User user, boolean isPlus) {
        PlayerStats playerStats = getStatsOrCreateNew(seasonId, leagueId, user);
        updateTotalBets(playerStats, isPlus);
        savePlayerStats(playerStats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void calculateStatsBasedOnEmptyBet(String seasonId, String leagueId, User user, Integer betSize, boolean isPlus) {
        processStatsBasedOnEmptyBet(seasonId, leagueId, user, betSize, isPlus);
        processStatsBasedOnEmptyBet(seasonId, TOTAL_ID, user, betSize, isPlus);
    }

    public void processStatsBasedOnEmptyBet(String seasonId, String leagueId, User user, Integer betSize, boolean isPlus) {
        PlayerStats playerStats = getStatsOrCreateNew(seasonId, leagueId, user);
        updateTotalBets(playerStats, isPlus);
        modifyEmptyBetValues(playerStats, betSize, isPlus);
        savePlayerStats(playerStats);
    }

    public void modifyEmptyBetValues(PlayerStats playerStats, Integer betSize, boolean isPlus) {
        updateEmptyBetValues(playerStats, betSize, isPlus);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void calculateStatsBasedOnBetResult(String seasonId, String leagueId, User user, Bet bet, boolean isPlus) {
        processStatsBasedOnBetResult(seasonId, leagueId, user, bet, isPlus);
        processStatsBasedOnBetResult(seasonId, TOTAL_ID, user, bet, isPlus);
    }

    public void processStatsBasedOnBetResult(String seasonId, String leagueId, User user, Bet bet, boolean isPlus) {
        PlayerStats playerStats = getStatsOrCreateNew(seasonId, leagueId, user);
        modifyStatsByBetResult(playerStats, bet, isPlus);
        savePlayerStats(playerStats);
    }

    public void modifyStatsByBetResult(PlayerStats playerStats, Bet bet, boolean isPlus) {
        updateBetCount(playerStats, isPlus);
        updateBetCountValuesBasedOnBetStatus(playerStats, bet.getBetStatus(), bet.getBetOdds(), isPlus);
        updateSumOfOddsAndActualBalance(playerStats, bet.getBetStatus(), bet.getBetOdds(), bet.getBalanceChange(), isPlus);
        recalculateStats(playerStats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void calculateStatsBasedOnEditedBet(String seasonId, String leagueId, User user, Bet bet, boolean isPlus) {
        processStatsBasedOnEditedBet(seasonId, leagueId, user, bet, isPlus);
        processStatsBasedOnEditedBet(seasonId, TOTAL_ID, user, bet, isPlus);
    }

    public void processStatsBasedOnEditedBet(String seasonId, String leagueId, User user, Bet bet, boolean isPlus) {
        PlayerStats playerStats = getStatsOrCreateNew(seasonId, leagueId, user);
        modifyEditedBetValues(playerStats, bet, isPlus);
        savePlayerStats(playerStats);
    }

    public void modifyEditedBetValues(PlayerStats playerStats, Bet bet, boolean isPlus) {
        updateTotalBets(playerStats, isPlus);
        updateBetCount(playerStats, isPlus);
        updateBetCountValuesBasedOnBetStatus(playerStats, bet.getBetStatus(), bet.getBetOdds(), isPlus);
        updateSumOfOddsAndActualBalance(playerStats, bet.getBetStatus(), bet.getBetOdds(), bet.getBalanceChange(), isPlus);
        recalculateStats(playerStats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void updateTotalBets(PlayerStats playerStats, boolean isPlus) {
        int value = isPlus ? 1 : -1;
        playerStats.setTotalBets(playerStats.getTotalBets() + value);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void savePlayerStats(PlayerStats playerStats) {
        playerStatsRepository.save(playerStats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStats getStatsOrCreateNew(String seasonId, String leagueId, User user) {
        return playerStatsRepository
                .findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user)
                .orElseGet(() -> createNewStats(seasonId, leagueId, user));
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStats createNewStats(String seasonId, String leagueId, User user) {
        return PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .user(user)
                .totalBets(0)
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
