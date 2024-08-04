package net.friendly_bets.utils;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GetEntityOrThrow {

    public static User getUserOrThrow(UsersRepository usersRepository, String userId) {
        return usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User", userId));
    }

    public static Season getSeasonOrThrow(SeasonsRepository seasonsRepository, String seasonId) {
        return seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Season", seasonId));
    }

    public static League getLeagueOrThrow(LeaguesRepository leaguesRepository, String leagueId) {
        return leaguesRepository.findById(leagueId).orElseThrow(
                () -> new NotFoundException("League", leagueId));
    }

    public static Team getTeamOrThrow(TeamsRepository teamsRepository, String teamId) {
        return teamsRepository.findById(teamId).orElseThrow(
                () -> new NotFoundException("Team", teamId));
    }

    public static Bet getBetOrThrow(BetsRepository betsRepository, String betId) {
        return betsRepository.findById(betId).orElseThrow(
                () -> new NotFoundException("Bet", betId));
    }

    public static CalendarNode getCalendarNodeOrThrow(CalendarsRepository calendarsRepository, String calendarNodeId) {
        return calendarsRepository.findById(calendarNodeId).orElseThrow(
                () -> new NotFoundException("CalendarNode", calendarNodeId));
    }

    public static PlayerStats getPlayerStatsOrThrow(PlayerStatsRepository playerStatsRepository, String seasonId, String leagueId, User user) {
        Optional<PlayerStats> playerStatsOptional =
                playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
        return playerStatsOptional.orElseThrow(() -> new BadRequestException("noTotalPlayerStats"));
    }

    public static PlayerStatsByTeams getPlayerStatsByTeamsOrThrow(PlayerStatsByTeamsRepository playerStatsByTeamsRepository, String seasonId, String leagueId, User user) {
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserAndIsLeagueStats(seasonId, leagueId, user, false);
        return playerStatsByTeamsOptional.orElseThrow(() -> new BadRequestException("noPlayerStatsByTeamsInLeague"));
    }

    public static PlayerStatsByTeams getTotalStatsByTeamsOrThrow(PlayerStatsByTeamsRepository playerStatsByTeamsRepository, String seasonId, String leagueId) {
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndIsLeagueStats(seasonId, leagueId, true);
        return playerStatsByTeamsOptional.orElseThrow(() -> new BadRequestException("noTotalStatsByTeamsInLeague"));
    }

    public static PlayerStatsByTeams getPlayerStatsByTeamsOrNull(PlayerStatsByTeamsRepository playerStatsByTeamsRepository, String seasonId, String leagueId, User user, boolean isLeagueStats) {
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserAndIsLeagueStats(seasonId, leagueId, user, isLeagueStats);
        return playerStatsByTeamsOptional.orElse(null);
    }

    public static PlayerStatsByTeams getLeagueStatsByTeamsOrNull(PlayerStatsByTeamsRepository playerStatsByTeamsRepository, String seasonId, String leagueId, boolean isLeagueStats) {
        Optional<PlayerStatsByTeams> leagueStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndIsLeagueStats(seasonId, leagueId, isLeagueStats);
        return leagueStatsByTeamsOptional.orElse(null);
    }

    public static List<CalendarNode> getListOfCalendarNodesBySeasonOrThrow(CalendarsRepository calendarsRepository, String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonId(seasonId);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }


    public static PlayerStats getDefaultPlayerStats(String seasonId, String leagueId, User user) {
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

    public static PlayerStatsByTeams getDefaultStatsByTeams(String seasonId, String leagueId, String leagueCode, User user, boolean isLeagueStats) {
        return PlayerStatsByTeams.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .leagueCode(leagueCode)
                .user(user)
                .teamStats(new ArrayList<>())
                .isLeagueStats(isLeagueStats)
                .build();
    }
}
