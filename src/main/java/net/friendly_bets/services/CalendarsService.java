package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.LeagueMatchdayNode;
import net.friendly_bets.repositories.CalendarsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.friendly_bets.utils.BetUtils.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CalendarsService {

    CalendarsRepository calendarsRepository;
    GetEntityService getEntityService;


    public CalendarNodesPage getAllSeasonCalendarNodes(String seasonId) {
        List<CalendarNode> calendarNodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(seasonId);
        calendarNodes.sort(Comparator.comparing(CalendarNode::getStartDate).reversed());

        return CalendarNodesPage.builder()
                .calendarNodes(CalendarNodeDto.from(calendarNodes, false))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public CalendarNodesPage getSeasonCalendarHasBetsNodes(String seasonId) {
        List<CalendarNode> calendarNodes = getEntityService.getListOfCalendarNodesWithBetsBySeasonOrThrow(seasonId);
        calendarNodes.sort(Comparator.comparing(CalendarNode::getStartDate).reversed());

        return CalendarNodesPage.builder()
                .calendarNodes(CalendarNodeDto.from(calendarNodes, false))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getActualCalendarNodeBets(String seasonId) {
        List<CalendarNode> calendarNodes = getEntityService.getListOfCalendarNodesWithBetsBySeasonOrThrow(seasonId);

        CalendarNode closestNode = calendarNodes.stream()
                .min(Comparator.comparing(node -> Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), node.getStartDate()))))
                .orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));

        return getBetsByCalendarNode(closestNode.getId());
    }

    // ------------------------------------------------------------------------------------------------------ //


    public CalendarNodeDto createCalendarNode(NewCalendarNodeDto newCalendarNode) {
        List<CalendarNode> calendarNodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(newCalendarNode.getSeasonId());
        datesRangeValidation(newCalendarNode.getStartDate(), newCalendarNode.getEndDate());
        leagueMatchdaysValidation(calendarNodes, newCalendarNode.getLeagueMatchdayNodes());

        CalendarNode calendarNode = CalendarNode.builder()
                .createdAt(LocalDateTime.now())
                .seasonId(newCalendarNode.getSeasonId())
                .startDate(newCalendarNode.getStartDate())
                .endDate(newCalendarNode.getEndDate())
                .leagueMatchdayNodes(newCalendarNode.getLeagueMatchdayNodes())
                .hasBets(false)
                .isFinished(false)
                .gameweekStats(new ArrayList<>())
                .build();

        saveCalendarNode(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public void addBetToCalendarNode(String betId, String calendarNodeId, String leagueId, String matchday) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);
        LeagueMatchdayNode node = getLeagueMatchdayNode(calendarNode, leagueId, matchday);
        Bet bet = getEntityService.getBetOrThrow(betId);

        checkLeagueBetLimit(node, bet.getUser().getId());
        node.getBets().add(bet);

        if (!calendarNode.getHasBets()) {
            calendarNode.setHasBets(true);
        }

        saveCalendarNode(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getBetsByCalendarNode(String calendarNodeId) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);

        List<Bet> bets = new ArrayList<>();
        for (LeagueMatchdayNode leagueMatchdayNode : calendarNode.getLeagueMatchdayNodes()) {
            bets.addAll(leagueMatchdayNode.getBets());
        }

        return BetsPage.builder()
                .bets(BetDto.from(bets))
                .totalPages(1)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public CalendarNodeDto deleteCalendarNode(String calendarNodeId) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);

        if (calendarNode.getHasBets()) {
            throw new BadRequestException("cannotDeleteCalendarNodeWithBets");
        }

        calendarsRepository.delete(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public CalendarNodeDto deleteBetInCalendarNode(String calendarNodeId, String betId) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);
        boolean betFound = false;

        for (LeagueMatchdayNode leagueMatchdayNode : calendarNode.getLeagueMatchdayNodes()) {
            List<Bet> bets = leagueMatchdayNode.getBets();
            betFound = bets.removeIf(bet -> bet.getId().equals(betId)) || betFound;
        }

        if (!betFound) {
            throw new BadRequestException("betNotFoundInCalendar");
        }

        boolean hasBets = calendarNode.getLeagueMatchdayNodes().stream()
                .anyMatch(leagueMatchdayNode -> !leagueMatchdayNode.getBets().isEmpty());

        calendarNode.setHasBets(hasBets);
        calendarNode.setIsFinished(false);

        saveCalendarNode(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    // TODO: удалить метод после рефакторинга базы данных (добавление calendarNodeId в ставки в старых сезонах)

    public CalendarNodeDto deleteBetInCalendars(String seasonId, String betId) {
        List<CalendarNode> calendarNodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(seasonId);

        CalendarNode updatedCalendarNode = null;
        boolean betFound = false;

        for (CalendarNode calendarNode : calendarNodes) {
            for (LeagueMatchdayNode leagueMatchdayNode : calendarNode.getLeagueMatchdayNodes()) {
                if (leagueMatchdayNode.getBets().removeIf(bet -> bet.getId().equals(betId))) {
                    betFound = true;
                    updatedCalendarNode = calendarNode;
                    break;
                }
            }
            if (betFound) {
                break;
            }
        }
        if (updatedCalendarNode == null) {
            throw new BadRequestException("betNotFoundInAnyCalendar");
        }
        boolean hasBets = updatedCalendarNode.getLeagueMatchdayNodes().stream()
                .anyMatch(leagueMatchdayNode -> !leagueMatchdayNode.getBets().isEmpty());

        updatedCalendarNode.setHasBets(hasBets);
        updatedCalendarNode.setIsFinished(false);

        saveCalendarNode(updatedCalendarNode);

        return CalendarNodeDto.from(updatedCalendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void updateCalendar(Bet bet, EditedBetDto editedBet) {
        String newCalendarNodeId = editedBet.getCalendarNodeId();
        String prevCalendarNodeId = editedBet.getPrevCalendarNodeId();

        if (newCalendarNodeId.equals(prevCalendarNodeId)) {
            CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(newCalendarNodeId);
            LeagueMatchdayNode node = getLeagueMatchdayNode(calendarNode, editedBet.getLeagueId(), editedBet.getMatchDay());
            checkLeagueBetLimit(node, editedBet.getUserId());
        } else {
            deleteBetFromCalendar(bet, prevCalendarNodeId);
            bet.setCalendarNodeId(newCalendarNodeId);
            addBetToCalendarNode(bet.getId(), newCalendarNodeId, editedBet.getLeagueId(), editedBet.getMatchDay());
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void deleteBetFromCalendar(Bet bet, String calendarNodeId) {
        // TODO: убрать временное решение после реализации календаря в прошлых сезонах. Оставить только deleteBetInCalendarNode
        if (calendarNodeId == null || calendarNodeId.isBlank()) {
            deleteBetInCalendars(bet.getSeason().getId(), bet.getId());
        } else {
            deleteBetInCalendarNode(calendarNodeId, bet.getId());
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    public LeagueMatchdayNode getLeagueMatchdayNode(String calendarNodeId, String leagueId, String matchday) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);
        return getEntityService.getLeagueMatchdayNodeOrThrow(calendarNode, leagueId, matchday);
    }

    public LeagueMatchdayNode getLeagueMatchdayNode(CalendarNode calendarNode, String leagueId, String matchday) {
        return getEntityService.getLeagueMatchdayNodeOrThrow(calendarNode, leagueId, matchday);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    private void saveCalendarNode(CalendarNode calendarNode) {
        calendarsRepository.save(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //


}
