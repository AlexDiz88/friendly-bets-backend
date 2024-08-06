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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.friendly_bets.utils.BetValuesUtils.datesRangeValidation;
import static net.friendly_bets.utils.BetValuesUtils.leagueMatchdaysValidation;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CalendarsServiceImpl implements CalendarsService {

    CalendarsRepository calendarsRepository;
    BetsRepository betsRepository;
    SeasonsRepository seasonsRepository;

    @Override
    public CalendarNodesPage getAllSeasonCalendarNodes(String seasonId) {
        List<CalendarNode> calendarNodes = getListOfCalendarNodesBySeasonOrThrow(calendarsRepository, seasonId);
        calendarNodes.sort(Comparator.comparing(CalendarNode::getStartDate).reversed());

        return CalendarNodesPage.builder()
                .calendarNodes(CalendarNodeDto.from(calendarNodes))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
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
                .build();

        calendarsRepository.save(calendarNode);

        return CalendarNodeDto.from(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Override
    @Transactional
    public CalendarNodeDto addBetToCalendarNode(String betId, String calendarNodeId, String leagueId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);
        Bet bet = getBetOrThrow(betsRepository, betId);
        List<LeagueMatchdayNode> leagueMatchdayNodes = calendarNode.getLeagueMatchdayNodes();
        boolean betAdded = false;
        for (LeagueMatchdayNode node : leagueMatchdayNodes) {
            if (node.getLeagueId().equals(leagueId)) {
                node.getBets().add(bet);
                betAdded = true;
                break;
            }
        }

        if (betAdded) {
            calendarsRepository.save(calendarNode);
        } else {
            throw new BadRequestException("leagueNotFoundInCalendarNode");
        }

        return CalendarNodeDto.from(calendarNode);
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
    @Transactional
    public CalendarNodeDto deleteCalendarNode(String calendarNodeId) {
        CalendarNode calendarNode = getCalendarNodeOrThrow(calendarsRepository, calendarNodeId);
        boolean hasBets = calendarNode.getLeagueMatchdayNodes().stream()
                .anyMatch(node -> !node.getBets().isEmpty());

        if (hasBets) {
            throw new BadRequestException("cannotDeleteCalendarNodeWithBets");
        }

        calendarsRepository.delete(calendarNode);

        return CalendarNodeDto.from(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //
}
