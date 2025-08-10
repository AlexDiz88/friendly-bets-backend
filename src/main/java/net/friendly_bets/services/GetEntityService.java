package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetEntityService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    UsersRepository usersRepository;
    TeamsRepository teamsRepository;
    BetsRepository betsRepository;
    CalendarsRepository calendarsRepository;
    PlayerStatsRepository playerStatsRepository;
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;

    public User getUserOrThrow(String userId) {
        return usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User", userId));
    }

    public Season getSeasonOrThrow(String seasonId) {
        return seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Season", seasonId));
    }

    public League getLeagueOrThrow(String leagueId) {
        return leaguesRepository.findById(leagueId).orElseThrow(
                () -> new NotFoundException("League", leagueId));
    }

    public Team getTeamOrThrow(String teamId) {
        return teamsRepository.findById(teamId).orElseThrow(
                () -> new NotFoundException("Team", teamId));
    }

    public Bet getBetOrThrow(String betId) {
        return betsRepository.findById(betId).orElseThrow(
                () -> new NotFoundException("Bet", betId));
    }

    public CalendarNode getCalendarNodeOrThrow(String calendarNodeId) {
        return calendarsRepository.findById(calendarNodeId).orElseThrow(
                () -> new NotFoundException("CalendarNode", calendarNodeId));
    }

    public LeagueMatchdayNode getLeagueMatchdayNodeOrThrow(CalendarNode calendarNode, String leagueId, String matchday) {
        return calendarNode.getLeagueMatchdayNodes().stream()
                .filter(node -> leagueId != null && leagueId.equals(node.getLeagueId())
                        && (matchday != null && matchday.equals(node.getMatchDay())))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInCalendarNode"));
    }

    public LeagueMatchdayNode getLeagueMatchdayNodeFromSeasonOrThrow(String seasonId, String leagueId, String matchday) {
        List<CalendarNode> calendarNodes = calendarsRepository.findBySeasonId(seasonId)
                .orElseThrow(() -> new BadRequestException("calendarNodesNotFoundForSeason"));

        return calendarNodes.stream()
                .flatMap(calendarNode -> calendarNode.getLeagueMatchdayNodes().stream())
                .filter(node -> leagueId != null && leagueId.equals(node.getLeagueId())
                        && matchday != null && matchday.equals(node.getMatchDay()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeasonCalendar"));
    }

    public PlayerStats getTotalPlayerStatsOrThrow(String seasonId, String leagueId, String userId) {
        return playerStatsRepository.findAllBySeasonIdAndLeagueIdAndUser_Id(seasonId, leagueId, userId)
                .orElseThrow(() -> new BadRequestException("playerStatsForSeasonNotFound"));
    }

    public PlayerStats getPlayerStatsOrNull(String seasonId, String leagueId, User user) {
        Optional<PlayerStats> playerStatsOptional =
                playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
        return playerStatsOptional.orElse(null);
    }

    public PlayerStatsByTeams getPlayerStatsByTeamsOrThrow(String seasonId, String leagueId, String userId) {
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional =
                playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
        return playerStatsByTeamsOptional.orElseThrow(() -> new BadRequestException("noPlayerStatsByTeamsInLeague"));
    }

    public List<CalendarNode> getListOfCalendarNodesBySeasonOrThrow(String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonId(seasonId);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }

    public List<CalendarNode> getListOfCalendarNodesWithBetsBySeasonOrThrow(String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonIdAndHasBets(seasonId, true);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }

    public List<CalendarNode> getListOfCalendarNodesIsFinishedOrThrow(String seasonId) {
        Optional<List<CalendarNode>> calendarNodesListOptional =
                calendarsRepository.findBySeasonIdAndIsFinished(seasonId, true);
        return calendarNodesListOptional.orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));
    }
}
