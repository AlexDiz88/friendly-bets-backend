package net.friendly_bets.utils;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;

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

    public static PlayerStats getPlayerStatsOrNull(PlayerStatsRepository playerStatsRepository, String seasonId, String leagueId, User user) {
        Optional<PlayerStats> playerStatsOptional =
                playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
        return playerStatsOptional.orElse(null);
    }

    public static PlayerStatsByTeams getPlayerStatsByTeamsOrThrow(PlayerStatsByTeamsRepository playerStatsByTeamsRepository, String seasonId, String leagueId, String userId) {
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
        return playerStatsByTeamsOptional.orElseThrow(() -> new BadRequestException("noPlayerStatsByTeamsInLeague"));
    }

    public static List<CalendarNode> getListOfCalendarNodesBySeasonOrThrow(CalendarsRepository calendarsRepository, String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonId(seasonId);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }

    public static List<CalendarNode> getListOfCalendarNodesWithBetsBySeasonOrThrow(CalendarsRepository calendarsRepository, String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonIdAndHasBets(seasonId, true);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }
}
