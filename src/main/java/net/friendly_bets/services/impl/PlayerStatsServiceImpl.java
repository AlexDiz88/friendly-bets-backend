package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.PlayerStatsRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.PlayerStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static net.friendly_bets.utils.GetEntityOrThrow.getDefaultPlayerStats;
import static net.friendly_bets.utils.GetEntityOrThrow.getSeasonOrThrow;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerStatsServiceImpl implements PlayerStatsService {

    PlayerStatsRepository playerStatsRepository;
    SeasonsRepository seasonsRepository;

    @Override
    public AllPlayersStatsPage getAllPlayersStatsBySeason(String seasonId) {
        List<PlayerStats> allStatsBySeasonId = playerStatsRepository.findAllBySeasonId(seasonId);
        if (allStatsBySeasonId.isEmpty()) {
            return new AllPlayersStatsPage(new ArrayList<>());
        }
        Map<String, PlayerStats> statsByUsername = new HashMap<>();
        for (PlayerStats playerStats : allStatsBySeasonId) {
            String mapKey = seasonId + playerStats.getUser().getUsername();

            PlayerStats existingStats = statsByUsername.getOrDefault(mapKey, getDefaultPlayerStats(seasonId, playerStats.getLeagueId(), playerStats.getUser()));

            existingStats.setTotalBets(existingStats.getTotalBets() + playerStats.getTotalBets());
            existingStats.setBetCount(existingStats.getBetCount() + playerStats.getBetCount());
            existingStats.setWonBetCount(existingStats.getWonBetCount() + playerStats.getWonBetCount());
            existingStats.setReturnedBetCount(existingStats.getReturnedBetCount() + playerStats.getReturnedBetCount());
            existingStats.setLostBetCount(existingStats.getLostBetCount() + playerStats.getLostBetCount());
            existingStats.setEmptyBetCount(existingStats.getEmptyBetCount() + playerStats.getEmptyBetCount());
            existingStats.setActualBalance(existingStats.getActualBalance() + playerStats.getActualBalance());
            existingStats.setSumOfOdds(existingStats.getSumOfOdds() + playerStats.getSumOfOdds());
            existingStats.setSumOfWonOdds(existingStats.getSumOfWonOdds() + playerStats.getSumOfWonOdds());
            existingStats.calculateWinRate();
            existingStats.calculateAverageOdds();
            existingStats.calculateAverageWonBetOdds();

            statsByUsername.put(mapKey, existingStats);
        }

        List<PlayerStats> combinedStats = new ArrayList<>(statsByUsername.values());

        return new AllPlayersStatsPage(PlayerStatsDto.from(combinedStats));
    }

    @Override
    public AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<PlayerStats> allStatsBySeasonId = playerStatsRepository.findAllBySeasonId(seasonId);
        Map<String, List<PlayerStats>> statsByLeague = new HashMap<>();

        for (PlayerStats playerStats : allStatsBySeasonId) {
            String leagueId = playerStats.getLeagueId();
            List<PlayerStats> leagueStats = statsByLeague.getOrDefault(leagueId, new ArrayList<>());
            leagueStats.add(playerStats);
            statsByLeague.put(leagueId, leagueStats);
        }

        List<LeagueStatsPage> leagueStatsList = new ArrayList<>();
        List<League> leagues = season.getLeagues();
        for (League league : leagues) {
            String leagueId = league.getId();
            List<PlayerStats> leaguePlayerStats = statsByLeague.getOrDefault(leagueId, Collections.emptyList());

            LeagueStatsPage leagueStatsDto = new LeagueStatsPage(SimpleLeagueDto.from(league), PlayerStatsDto.from(leaguePlayerStats));
            leagueStatsList.add(leagueStatsDto);
        }

        return new AllPlayersStatsByLeaguesDto(leagueStatsList);
    }

    @Override
    @Transactional
    public AllPlayersStatsPage playersStatsFullRecalculation(String seasonId) {
        playerStatsRepository.deleteAllBySeasonId(seasonId);
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<League> leagues = season.getLeagues();
        Map<String, PlayerStats> statsMap = new HashMap<>();
        for (League league : leagues) {
            List<Bet> bets = league.getBets();
            for (Bet bet : bets) {
                if (bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
                    continue;
                }
                User user = bet.getUser();
                String mapKey = seasonId + league.getId() + user.getId();
                PlayerStats playerStats = statsMap.getOrDefault(mapKey, getDefaultPlayerStats(seasonId, league.getId(), user));

                playerStats.setTotalBets(playerStats.getTotalBets() + 1);
                if (bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
                    continue;
                }

                playerStats.setBetCount(playerStats.getBetCount() + 1);

                if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY)) {
                    playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + 1);
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                    playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
                    playerStats.setSumOfOdds(playerStats.getSumOfOdds() + bet.getBetOdds());
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                    playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
                    playerStats.setSumOfOdds(playerStats.getSumOfOdds() + bet.getBetOdds());
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
                    playerStats.setSumOfOdds(playerStats.getSumOfOdds() + bet.getBetOdds());
                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + bet.getBetOdds());
                }

                playerStats.setActualBalance(playerStats.getActualBalance() + bet.getBalanceChange());
                playerStats.calculateWinRate();
                playerStats.calculateAverageOdds();
                playerStats.calculateAverageWonBetOdds();

                statsMap.put(mapKey, playerStats);
            }
        }
        for (PlayerStats value : statsMap.values()) {
            playerStatsRepository.save(value);
        }
        return getAllPlayersStatsBySeason(seasonId);
    }
}
