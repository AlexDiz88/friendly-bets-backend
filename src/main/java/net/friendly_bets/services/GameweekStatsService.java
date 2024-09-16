package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.CalendarsRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.friendly_bets.utils.Constants.COMPLETED_BET_STATUSES;
import static net.friendly_bets.utils.Constants.NO_PREVIOUS_CALENDAR_NODE;
import static net.friendly_bets.utils.GetEntityOrThrow.*;


@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameweekStatsService {

    CalendarsRepository calendarsRepository;
    SeasonsRepository seasonsRepository;

    public void recalculateAllGameweekStats(String seasonId) {
        List<CalendarNode> calendarNodeList = getListOfCalendarNodesWithBetsBySeasonOrThrow(calendarsRepository, seasonId);
        calendarNodeList.sort(Comparator.comparing(CalendarNode::getStartDate));

        for (CalendarNode node : calendarNodeList) {
            calculateGameweekStats(node.getId());
        }
    }

    public void calculateGameweekStats(String calendarNodeId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);
        updateGameweekFinishedStatus(calendarNode);

        if (calendarNode.getIsFinished()) {
            updatePreviousGameweekId(calendarNode);
            updateGameweekStats(calendarNode);
            updatePlayersPositions(calendarNode);
            saveCalendarNode(calendarNode);
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void updateGameweekStats(CalendarNode calendarNode) {
        List<GameweekStats> gameweekStatsList = new ArrayList<>();

        for (LeagueMatchdayNode leagueMatchdayNode : calendarNode.getLeagueMatchdayNodes()) {
            for (Bet bet : leagueMatchdayNode.getBets()) {
                GameweekStats stats = getGameweekStatsOrCreateNew(gameweekStatsList, bet.getUser().getId());
                if (COMPLETED_BET_STATUSES.contains(bet.getBetStatus())) {
                    updateUserGameweekBalance(stats, bet.getBalanceChange());
                    updateUserTotalBalance(calendarNode.getPreviousGameweekId(), stats);
                }
            }
        }
        calendarNode.setGameweekStats(gameweekStatsList);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void updateGameweekFinishedStatus(CalendarNode calendarNode) {
        Season season = getSeasonOrThrow(seasonsRepository, calendarNode.getSeasonId());
        int totalBetsInGameweek = season.getPlayers().size() * calendarNode.getLeagueMatchdayNodes().size() * season.getBetCountPerMatchDay();

        long completedBetsCount = calendarNode.getLeagueMatchdayNodes().stream()
                .flatMap(node -> node.getBets().stream())
                .filter(bet -> COMPLETED_BET_STATUSES.contains(bet.getBetStatus()))
                .count();

        if (completedBetsCount > totalBetsInGameweek) {
            throw new BadRequestException("wrongBetsGameweekCount");
        }

        calendarNode.setIsFinished(completedBetsCount == totalBetsInGameweek);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void updatePreviousGameweekId(CalendarNode calendarNode) {
        List<CalendarNode> finishedCalendarNodes = getListOfCalendarNodesIsFinishedOrThrow(calendarsRepository, calendarNode.getSeasonId());
        CalendarNode closestPreviousNode = null;
        LocalDate currentNodeDate = calendarNode.getStartDate();

        for (CalendarNode finishedNode : finishedCalendarNodes) {
            if (finishedNode.getStartDate().isBefore(currentNodeDate)) {
                if (closestPreviousNode == null || finishedNode.getStartDate().isAfter(closestPreviousNode.getStartDate())) {
                    closestPreviousNode = finishedNode;
                }
            }
        }

        if (closestPreviousNode != null) {
            calendarNode.setPreviousGameweekId(closestPreviousNode.getId());
        } else {
            calendarNode.setPreviousGameweekId(NO_PREVIOUS_CALENDAR_NODE);
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void updateUserGameweekBalance(GameweekStats stats, double balanceChange) {
        stats.setBalanceChange(stats.getBalanceChange() == null ? balanceChange : stats.getBalanceChange() + balanceChange);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void updateUserTotalBalance(String prevCalendarNodeId, GameweekStats currentGameweekStats) {
        if (prevCalendarNodeId.equals(NO_PREVIOUS_CALENDAR_NODE)) {
            currentGameweekStats.setTotalBalance(currentGameweekStats.getBalanceChange());
        } else {
            CalendarNode prevCalendarNode = getCalendarNodeOrThrow(calendarsRepository, prevCalendarNodeId);
            List<GameweekStats> prevGameweekStats = prevCalendarNode.getGameweekStats();

            for (GameweekStats prevStats : prevGameweekStats) {
                if (prevStats.getUserId().equals(currentGameweekStats.getUserId())) {
                    if (prevStats.getTotalBalance() == null) {
                        currentGameweekStats.setTotalBalance(currentGameweekStats.getBalanceChange());
                    } else {
                        Double updatedBalance = prevStats.getTotalBalance() + currentGameweekStats.getBalanceChange();
                        currentGameweekStats.setTotalBalance(updatedBalance);
                    }
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void updatePlayersPositions(CalendarNode calendarNode) {
        List<GameweekStats> currentGameweekStats = calendarNode.getGameweekStats();
        String prevCalendarNodeId = calendarNode.getPreviousGameweekId();

        currentGameweekStats.sort((stats1, stats2) -> stats2.getTotalBalance().compareTo(stats1.getTotalBalance()));
        int position = 1;

        if (prevCalendarNodeId.equals(NO_PREVIOUS_CALENDAR_NODE)) {
            for (GameweekStats currentStats : currentGameweekStats) {
                currentStats.setPositionAfterGameweek(position);
                position++;
            }
        } else {
            CalendarNode prevCalendarNode = getCalendarNodeOrThrow(calendarsRepository, prevCalendarNodeId);
            List<GameweekStats> prevGameweekStats = prevCalendarNode.getGameweekStats();

            for (GameweekStats currentStats : currentGameweekStats) {
                currentStats.setPositionAfterGameweek(position);

                for (GameweekStats prevStats : prevGameweekStats) {
                    if (prevStats.getUserId().equals(currentStats.getUserId())) {
                        int positionChange = prevStats.getPositionAfterGameweek() - currentStats.getPositionAfterGameweek();
                        currentStats.setPositionChange(positionChange);
                        break;
                    }
                }
                position++;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    private void saveCalendarNode(CalendarNode calendarNode) {
        calendarsRepository.save(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private GameweekStats getGameweekStatsOrCreateNew(List<GameweekStats> gameweekStatsList, String userId) {
        return gameweekStatsList.stream()
                .filter(s -> s.getUserId().equals(userId))
                .findFirst()
                .orElseGet(() -> {
                    GameweekStats newStats = GameweekStats.builder()
                            .userId(userId)
                            .balanceChange(0.0)
                            .totalBalance(0.0)
                            .positionAfterGameweek(0)
                            .positionChange(0)
                            .build();
                    gameweekStatsList.add(newStats);
                    return newStats;
                });
    }

    // ------------------------------------------------------------------------------------------------------ //

}
