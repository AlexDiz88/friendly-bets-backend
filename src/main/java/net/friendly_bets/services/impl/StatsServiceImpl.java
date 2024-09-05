package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.StatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static net.friendly_bets.utils.Constants.WRL_STATUSES;
import static net.friendly_bets.utils.GetEntityOrThrow.*;
import static net.friendly_bets.utils.StatsUtils.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsServiceImpl implements StatsService {

    PlayerStatsRepository playerStatsRepository;
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;
    SeasonsRepository seasonsRepository;
    BetsRepository betsRepository;

    PlayerStatsService playerStatsService;
    TeamStatsService teamStatsService;
    GameweekStatsService gameweekStatsService;

    @Override
    public AllPlayersStatsPage getAllPlayersStatsBySeason(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);

        List<PlayerStats> resultStats = season.getPlayers().stream()
                .map(user -> getPlayerStatsOrNull(playerStatsRepository, seasonId, TOTAL_ID, user))
                .filter(playerStats -> playerStats != null && playerStats.getTotalBets() > 0)
                .collect(Collectors.toList());

        return new AllPlayersStatsPage(PlayerStatsDto.from(resultStats));
    }

    @Override
    public AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<PlayerStats> allStatsBySeasonId = playerStatsRepository.findAllBySeasonId(seasonId);

        Map<String, List<PlayerStats>> statsByLeague = allStatsBySeasonId.stream()
                .collect(Collectors.groupingBy(PlayerStats::getLeagueId));

        List<LeagueStatsPage> leagueStatsList = season.getLeagues().stream()
                .map(league -> {
                    String leagueId = league.getId();
                    List<PlayerStats> leaguePlayerStats = statsByLeague.getOrDefault(leagueId, Collections.emptyList());

                    List<PlayerStats> filteredStats = leaguePlayerStats.stream()
                            .filter(stats -> stats.getTotalBets() > 0)
                            .collect(Collectors.toList());

                    return new LeagueStatsPage(LeagueSimpleDto.from(league), PlayerStatsDto.from(filteredStats));
                })
                .filter(leagueStatsPage -> !leagueStatsPage.getPlayersStats().isEmpty())
                .collect(Collectors.toList());

        return new AllPlayersStatsByLeaguesDto(leagueStatsList);
    }


    @Override
    public AllStatsByTeamsInSeasonDto getAllStatsByTeamsInSeason(String seasonId) {
        List<PlayerStatsByTeams> allStatsByTeams = playerStatsByTeamsRepository.findAllBySeasonId(seasonId).orElseThrow(
                () -> new NotFoundException("Season", seasonId));

        return new AllStatsByTeamsInSeasonDto(PlayerStatsByTeamsDto.from(allStatsByTeams));
    }

    @Override
    public StatsByTeamsDto getStatsByTeams(String seasonId, String leagueId, String userId) {
        PlayerStatsByTeams statsByTeams = getPlayerStatsByTeamsOrThrow(playerStatsByTeamsRepository, seasonId, leagueId, userId);

        return new StatsByTeamsDto(PlayerStatsByTeamsDto.from(statsByTeams));
    }

    @Override
    @Transactional
    public AllPlayersStatsPage playersStatsFullRecalculation(String seasonId) {
        playerStatsRepository.deleteAllBySeasonId(seasonId);
        Map<String, PlayerStats> statsMap = new HashMap<>();
        List<Bet> bets = betsRepository.findAllBySeason_Id(seasonId);

        for (Bet bet : bets) {
            if (bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
                continue;
            }

            User user = bet.getUser();
            String leagueId = bet.getLeague().getId();
            String mapKey = seasonId + leagueId + user.getId();
            String totalMapKey = seasonId + TOTAL_ID + user.getId();

            Bet.BetStatus betStatus = bet.getBetStatus();

            PlayerStats leaguePlayerStats = statsMap.getOrDefault(mapKey, playerStatsService.createNewStats(seasonId, leagueId, user));
            PlayerStats totalPlayerStats = statsMap.getOrDefault(totalMapKey, playerStatsService.createNewStats(seasonId, TOTAL_ID, user));

            updatePlayerStats(leaguePlayerStats, bet, betStatus);
            statsMap.put(mapKey, leaguePlayerStats);

            updatePlayerStats(totalPlayerStats, bet, betStatus);
            statsMap.put(totalMapKey, totalPlayerStats);
        }

        for (PlayerStats value : statsMap.values()) {
            playerStatsRepository.save(value);
        }

        return getAllPlayersStatsBySeason(seasonId);
    }

    private void updatePlayerStats(PlayerStats playerStats, Bet bet, Bet.BetStatus betStatus) {
        playerStatsService.updateTotalBets(playerStats, true);

        if (Bet.BetStatus.OPENED == betStatus) {
            return;
        }

        if (Bet.BetStatus.EMPTY == betStatus) {
            updateEmptyBetValues(playerStats, bet.getBetSize(), true);
        }

        if (WRL_STATUSES.contains(betStatus)) {
            updateBetCount(playerStats, true);
            updateBetCountValuesBasedOnBetStatus(playerStats, betStatus, bet.getBetOdds(), true);
            updateSumOfOddsAndActualBalance(playerStats, bet.getBetStatus(), bet.getBetOdds(), bet.getBalanceChange(), true);
        }

        recalculateStats(playerStats);
    }

    @Override
    @Transactional
    public void playersStatsByTeamsRecalculation(String seasonId) {
        playerStatsByTeamsRepository.deleteAllBySeasonId(seasonId);
        Map<String, PlayerStatsByTeams> statsMap = new HashMap<>();
        List<Bet> bets = betsRepository.findAllBySeason_Id(seasonId);

        for (Bet bet : bets) {
            if (!WRL_STATUSES.contains(bet.getBetStatus())) {
                continue;
            }

            String userId = bet.getUser().getId();
            String leagueId = bet.getLeague().getId();
            String leagueMapKey = seasonId + leagueId + TOTAL_ID;
            String playerMapKey = seasonId + leagueId + userId;

            PlayerStatsByTeams leaguesStatsByTeams =
                    statsMap.getOrDefault(leagueMapKey, teamStatsService.createNewStatsByTeams(seasonId, leagueId, TOTAL_ID));
            PlayerStatsByTeams playersStatsByTeams =
                    statsMap.getOrDefault(playerMapKey, teamStatsService.createNewStatsByTeams(seasonId, leagueId, userId));

            teamStatsService.processTeamsStats(leaguesStatsByTeams, bet, true);
            statsMap.put(leagueMapKey, leaguesStatsByTeams);
            teamStatsService.processTeamsStats(playersStatsByTeams, bet, true);
            statsMap.put(playerMapKey, playersStatsByTeams);
        }

        for (PlayerStatsByTeams stats : statsMap.values()) {
            playerStatsByTeamsRepository.save(stats);
        }
    }

    @Override
    public void recalculateAllGameweekStats(String seasonId) {
        gameweekStatsService.recalculateAllGameweekStats(seasonId);
    }
}
