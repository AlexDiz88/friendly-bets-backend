package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.LeagueMatchdayNode;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.CalendarsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.friendly_bets.utils.BetUtils.datesRangeValidation;
import static net.friendly_bets.utils.BetUtils.leagueMatchdaysValidation;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CalendarsServiceImpl implements CalendarsService {

    CalendarsRepository calendarsRepository;
    BetsRepository betsRepository;
    SeasonsRepository seasonsRepository;

    @Override
    public CalendarNodesPage getAllSeasonCalendarNodes(String seasonId) {
        List<CalendarNode> calendarNodes = getListOfCalendarNodesBySeasonOrThrow(calendarsRepository, seasonId);
        calendarNodes.sort(Comparator.comparing(CalendarNode::getStartDate).reversed());

        return CalendarNodesPage.builder()
                .calendarNodes(CalendarNodeDto.from(calendarNodes, false))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public CalendarNodesPage getSeasonCalendarHasBetsNodes(String seasonId) {
        List<CalendarNode> calendarNodes = getListOfCalendarNodesWithBetsBySeasonOrThrow(calendarsRepository, seasonId);
        calendarNodes.sort(Comparator.comparing(CalendarNode::getStartDate).reversed());

        return CalendarNodesPage.builder()
                .calendarNodes(CalendarNodeDto.from(calendarNodes, false))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getActualCalendarNodeBets(String seasonId) {
        List<CalendarNode> calendarNodes = getListOfCalendarNodesWithBetsBySeasonOrThrow(calendarsRepository, seasonId);

        CalendarNode closestNode = calendarNodes.stream()
                .min(Comparator.comparing(node -> Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), node.getStartDate()))))
                .orElseThrow(() -> new BadRequestException("noCalendarNodesBySeason"));

        return getBetsByCalendarNode(closestNode.getId());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public CalendarNodeDto createCalendarNode(NewCalendarNodeDto newCalendarNode) {
        getSeasonOrThrow(seasonsRepository, newCalendarNode.getSeasonId());
        datesRangeValidation(newCalendarNode.getStartDate(), newCalendarNode.getEndDate());
        leagueMatchdaysValidation(calendarsRepository, newCalendarNode.getLeagueMatchdayNodes(), newCalendarNode.getSeasonId());

        CalendarNode calendarNode = CalendarNode.builder()
                .createdAt(LocalDateTime.now())
                .seasonId(newCalendarNode.getSeasonId())
                .startDate(newCalendarNode.getStartDate())
                .endDate(newCalendarNode.getEndDate())
                .leagueMatchdayNodes(newCalendarNode.getLeagueMatchdayNodes())
                .hasBets(false)
                .build();

        calendarsRepository.save(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Override
    public CalendarNodeDto addBetToCalendarNode(String betId, String calendarNodeId, String leagueId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);
        Bet bet = getBetOrThrow(betsRepository, betId);
        List<LeagueMatchdayNode> leagueMatchdayNodes = calendarNode.getLeagueMatchdayNodes();
        boolean betAdded = false;
        for (LeagueMatchdayNode node : leagueMatchdayNodes) {
            if (leagueId != null && leagueId.equals(node.getLeagueId())) {
                node.getBets().add(bet);
                betAdded = true;
                break;
            }
        }

        if (betAdded) {
            if (!calendarNode.getHasBets()) {
                calendarNode.setHasBets(true);
            }
            calendarsRepository.save(calendarNode);
        } else {
            throw new BadRequestException("leagueNotFoundInCalendarNode");
        }

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getBetsByCalendarNode(String calendarNodeId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);

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

    @Override
    public CalendarNodeDto deleteCalendarNode(String calendarNodeId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);

        if (calendarNode.getHasBets()) {
            throw new BadRequestException("cannotDeleteCalendarNodeWithBets");
        }

        calendarsRepository.delete(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public CalendarNodeDto deleteBetInCalendarNode(String calendarNodeId, String betId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);
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

        calendarsRepository.save(calendarNode);

        return CalendarNodeDto.from(calendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    // TODO: удалить метод после рефакторинга базы данных (добавление calendarNodeId в ставки в старых сезонах)
    @Override
    public CalendarNodeDto deleteBetInCalendars(String seasonId, String betId) {
        List<CalendarNode> calendarNodes = getListOfCalendarNodesBySeasonOrThrow(calendarsRepository, seasonId);

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

        calendarsRepository.save(updatedCalendarNode);

        return CalendarNodeDto.from(updatedCalendarNode, false);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void updateCalendar(Bet bet, EditedBetDto editedBet) {
        if (!editedBet.getCalendarNodeId().equals(editedBet.getPrevCalendarNodeId())) {
            deleteBetFromCalendar(bet, editedBet.getPrevCalendarNodeId());
            bet.setCalendarNodeId(editedBet.getCalendarNodeId());
            addBetToCalendarNode(bet.getId(), editedBet.getCalendarNodeId(), editedBet.getLeagueId());
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    public void deleteBetFromCalendar(Bet bet, String calendarNodeId) {
        // TODO: убрать временное решение. Оставить только deleteBetInCalendarNode
        if (calendarNodeId == null || calendarNodeId.isBlank()) {
            deleteBetInCalendars(bet.getSeason().getId(), bet.getId());
        } else {
            deleteBetInCalendarNode(calendarNodeId, bet.getId());
        }
    }

    // ------------------------------------------------------------------------------------------------------ //


}
