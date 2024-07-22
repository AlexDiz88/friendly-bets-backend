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
import net.friendly_bets.services.PlayerStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerStatsServiceImpl implements PlayerStatsService {

    PlayerStatsRepository playerStatsRepository;
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;
    SeasonsRepository seasonsRepository;
    BetsRepository betsRepository;

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
    public AllStatsByTeamsInSeasonDto getAllStatsByTeamsInSeason(String seasonId) {
        List<PlayerStatsByTeams> allStatsByTeams = playerStatsByTeamsRepository.findAllBySeasonId(seasonId).orElseThrow(
                () -> new NotFoundException("Season", seasonId)
        );

        return new AllStatsByTeamsInSeasonDto(PlayerStatsByTeamsDto.from(allStatsByTeams));
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
            String mapKey = seasonId + bet.getLeague().getId() + user.getId();
            PlayerStats playerStats = statsMap.getOrDefault(mapKey, getDefaultPlayerStats(seasonId, bet.getLeague().getId(), user));

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

        for (PlayerStats value : statsMap.values()) {
            playerStatsRepository.save(value);
        }
        return getAllPlayersStatsBySeason(seasonId);
    }

    @Override
    @Transactional
    public void playersStatsByTeamsRecalculation(String seasonId) {
        playerStatsByTeamsRepository.deleteAllBySeasonId(seasonId);
        Map<String, PlayerStatsByTeams> statsMap = new HashMap<>();
        List<Bet> bets = betsRepository.findAllBySeason_Id(seasonId);
        EnumSet<Bet.BetStatus> ignoredStatuses = EnumSet.of(Bet.BetStatus.DELETED, Bet.BetStatus.OPENED, Bet.BetStatus.EMPTY);

        for (Bet bet : bets) {
            if (ignoredStatuses.contains(bet.getBetStatus())) {
                continue;
            }
            User user = bet.getUser();
            String leagueId = bet.getLeague().getId();
            String mapKey = seasonId + leagueId + user.getId();
            String mapKeyForLeagues = seasonId + leagueId;
            PlayerStatsByTeams leaguesStatsByTeams =
                    statsMap.getOrDefault(mapKeyForLeagues, getDefaultStatsByTeams(seasonId, leagueId, bet.getLeague().getDisplayNameRu(), user, true));
            PlayerStatsByTeams playersStatsByTeams =
                    statsMap.getOrDefault(mapKey, getDefaultStatsByTeams(seasonId, leagueId, bet.getLeague().getDisplayNameRu(), user, false));

            PlayerStatsByTeams leagueStatsByTeams = calculateTeamsStats(bet, leaguesStatsByTeams);
            PlayerStatsByTeams statsByTeams = calculateTeamsStats(bet, playersStatsByTeams);
            statsMap.put(mapKeyForLeagues, leagueStatsByTeams);
            statsMap.put(mapKey, statsByTeams);

            for (PlayerStatsByTeams value : statsMap.values()) {
                playerStatsByTeamsRepository.save(value);
            }
        }
    }

    public static PlayerStatsByTeams calculateTeamsStats(Bet bet, PlayerStatsByTeams playerStatsByTeams) {
        for (Team team : Arrays.asList(bet.getHomeTeam(), bet.getAwayTeam())) {
            Optional<TeamStats> optionalTeamStats = Optional.ofNullable(playerStatsByTeams)
                    .map(PlayerStatsByTeams::getTeamStats)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(stats -> stats.getTeam() != null && stats.getTeam().equals(team))
                    .findFirst();


            if (optionalTeamStats.isPresent()) {
                // обновление существующей статистики игрока по команде
                TeamStats teamStats = optionalTeamStats.get();
                teamStats.setBetCount(teamStats.getBetCount() + 1);
                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    teamStats.setWonBetCount(teamStats.getWonBetCount() + 1);
                    teamStats.setSumOfWonOdds(teamStats.getSumOfWonOdds() + bet.getBetOdds());
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                    teamStats.setReturnedBetCount(teamStats.getReturnedBetCount() + 1);
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                    teamStats.setLostBetCount(teamStats.getLostBetCount() + 1);
                }
                teamStats.setSumOfOdds(teamStats.getSumOfOdds() + bet.getBetOdds());
                teamStats.setActualBalance(teamStats.getActualBalance() + bet.getBalanceChange());
                teamStats.calculateWinRate();
                teamStats.calculateAverageOdds();
                teamStats.calculateAverageWonBetOdds();
            } else {
                // Создание нового TeamStats и добавление в список
                TeamStats newTeamStats = TeamStats.builder()
                        .team(team)
                        .betCount(1)
                        .wonBetCount(0)
                        .returnedBetCount(0)
                        .lostBetCount(0)
                        .sumOfOdds(0.0)
                        .sumOfWonOdds(0.0)
                        .build();
                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    newTeamStats.setWonBetCount(1);
                    newTeamStats.setSumOfWonOdds(bet.getBetOdds());
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                    newTeamStats.setReturnedBetCount(1);
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                    newTeamStats.setLostBetCount(1);
                }
                newTeamStats.setSumOfOdds(bet.getBetOdds());
                newTeamStats.setActualBalance(bet.getBalanceChange());
                newTeamStats.calculateWinRate();
                newTeamStats.calculateAverageOdds();
                newTeamStats.calculateAverageWonBetOdds();

                if (playerStatsByTeams != null) {
                    playerStatsByTeams.getTeamStats().add(newTeamStats);
                }
            }
        }
        return playerStatsByTeams;
    }
}
