package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetValueRangeStats;
import net.friendly_bets.models.League;
import net.friendly_bets.models.PlayerStatsByBetValues;
import net.friendly_bets.models.enums.BetValueRange;
import net.friendly_bets.repositories.PlayerStatsByBetValuesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static net.friendly_bets.utils.StatsUtils.recalculateStats;
import static net.friendly_bets.utils.StatsUtils.updateBetCount;
import static net.friendly_bets.utils.StatsUtils.updateBetCountValuesBasedOnBetStatus;
import static net.friendly_bets.utils.StatsUtils.updateSumOfOddsAndActualBalance;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetValueStatsService {

    PlayerStatsByBetValuesRepository playerStatsByBetValuesRepository;

    public void calculateStatsByBetValues(
            String seasonId,
            String leagueId,
            League.LeagueCode leagueCode,
            String userId,
            Bet bet,
            boolean isPlus
    ) {
        processStatsByBetValues(seasonId, leagueId, leagueCode, userId, bet, isPlus);
        processStatsByBetValues(seasonId, TOTAL_ID, null, userId, bet, isPlus);
    }

    public void applyBetToStats(PlayerStatsByBetValues stats, Bet bet, boolean isPlus) {
        BetValueRange range = BetValueRange.fromOdds(requireOdds(bet));
        BetValueRangeStats rangeStats = findRangeStats(stats, range);
        modifyRangeStats(rangeStats, bet, isPlus);
        refreshTotals(stats);
    }

    public PlayerStatsByBetValues createNewStats(
            String seasonId,
            String leagueId,
            League.LeagueCode leagueCode,
            String userId
    ) {
        List<BetValueRangeStats> rangeStats = new ArrayList<>();
        for (BetValueRange range : BetValueRange.values()) {
            rangeStats.add(createEmptyRangeStats(range));
        }
        return PlayerStatsByBetValues.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .leagueCode(leagueCode)
                .userId(userId)
                .betCount(0)
                .actualBalance(0.0)
                .rangeStats(rangeStats)
                .build();
    }

    private void processStatsByBetValues(
            String seasonId,
            String leagueId,
            League.LeagueCode leagueCode,
            String userId,
            Bet bet,
            boolean isPlus
    ) {
        PlayerStatsByBetValues stats = getStatsOrCreateNew(seasonId, leagueId, leagueCode, userId);
        applyBetToStats(stats, bet, isPlus);
        saveStats(stats);
    }

    private PlayerStatsByBetValues getStatsOrCreateNew(
            String seasonId,
            String leagueId,
            League.LeagueCode leagueCode,
            String userId
    ) {
        return playerStatsByBetValuesRepository
                .findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId)
                .orElseGet(() -> createNewStats(seasonId, leagueId, leagueCode, userId));
    }

    private BetValueRangeStats findRangeStats(PlayerStatsByBetValues stats, BetValueRange range) {
        return stats.getRangeStats().stream()
                .filter(rangeStats -> rangeStats.getRange() == range)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bet value range not found: " + range));
    }

    private void modifyRangeStats(BetValueRangeStats stats, Bet bet, boolean isPlus) {
        updateBetCount(stats, isPlus);
        updateBetCountValuesBasedOnBetStatus(stats, bet.getBetStatus(), bet.getBetOdds(), isPlus);
        updateSumOfOddsAndActualBalance(stats, bet.getBetStatus(), bet.getBetOdds(), bet.getBalanceChange(), isPlus);
        recalculateStats(stats);
    }

    private void refreshTotals(PlayerStatsByBetValues stats) {
        int betCount = 0;
        double actualBalance = 0.0;
        for (BetValueRangeStats rangeStats : stats.getRangeStats()) {
            betCount += rangeStats.getBetCount();
            actualBalance += rangeStats.getActualBalance();
        }
        stats.setBetCount(betCount);
        stats.setActualBalance(actualBalance);
    }

    private BetValueRangeStats createEmptyRangeStats(BetValueRange range) {
        return BetValueRangeStats.builder()
                .range(range)
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

    private double requireOdds(Bet bet) {
        Double odds = bet.getBetOdds();
        if (odds == null || odds <= 0) {
            throw new BadRequestException("betCoefNotSpecified");
        }
        return odds;
    }

    @Transactional
    private void saveStats(PlayerStatsByBetValues stats) {
        playerStatsByBetValuesRepository.save(stats);
    }
}
