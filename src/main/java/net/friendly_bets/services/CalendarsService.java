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


    public void addBetToCalendarNode(Bet bet, String calendarNodeId, String leagueId, String matchday) {
        CalendarNode calendarNode = getEntityService.getCalendarNodeOrThrow(calendarNodeId);
        LeagueMatchdayNode node = getLeagueMatchdayNode(calendarNode, leagueId, matchday);

        checkLeagueBetLimit(node, bet.getUser().getId());

        node.getBets().add(bet);
        calendarNode.setHasBets(true);

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

    public void updateCalendar(Bet prevBet, Bet newBet) {
        String prevCalendarNodeId = prevBet.getCalendarNodeId();
        String newCalendarNodeId = newBet.getCalendarNodeId();
        String prevMatchDay = prevBet.getMatchDay();
        String newMatchDay = newBet.getMatchDay();
        String prevUserId = prevBet.getUser().getId();
        String newUserId = newBet.getUser().getId();

        boolean sameMatchDay = newMatchDay.equals(prevMatchDay);
        boolean sameUser = newUserId.equals(prevUserId);

        // Given: LeagueMatchdayNodes АПЛ1, АПЛ2, АПЛ3, БЛ1.
        // Given: CalendarNode1 (содержит АПЛ1, АПЛ2), CalendarNode2 (содержит АПЛ3, БЛ1).
        // Во время редактирования лигу изменить невозможно. Но можно поменять matchDay той же лиги, например АПЛ1 -> АПЛ2, либо поменять user.
        // Любое из этих двух изменений могут повлиять на CalendarNode и LeagueMatchdayNode!

        // 1. Если редактирование внутри одной CalendarNode (напр. CalendarNode1) и одной LeagueMatchdayNode (АПЛ1), т.е. тот же matchDay:
        // 1.1. Изменились любые параметры кроме user -> Просто редактируем, НЕ проверяя лимит и ничего не удаляем.
        // 1.2. Изменился user -> проверяем лимит для нового юзера в этой LeagueMatchdayNode (АПЛ1) для CalendarNode1.
        // Если проверка пройдена - добавляем для нового user ставку в LeagueMatchdayNode(АПЛ1) и удаляем старую ставку из LeagueMatchdayNode(АПЛ1) для предыдущего user.

        // 2. Если редактирование внутри одной CalendarNode, но другой LeagueMatchdayNode (АПЛ1 -> АПЛ2), т.е.поменялся matchDay:
        // 2.1 Помимо matchDay изменились любые параметры кроме user -> проверяем лимит для этого юзера в новой LeagueMatchdayNode (АПЛ2) для CalendarNode1
        // Если проверка пройдена - добавляем ставку в новую LeagueMatchdayNode(АПЛ2) и удаляем старую ставку из LeagueMatchdayNode(АПЛ1) для этого user.
        // 2.2 Изменился user -> проверяем лимит для нового юзера в новой LeagueMatchdayNode (АПЛ2) для CalendarNode1
        // Если проверка пройдена - добавляем для нового user ставку в LeagueMatchdayNode(АПЛ2) и удаляем старую ставку из LeagueMatchdayNode(АПЛ1) для предыдущего user.

        // 3. Если редактирование в другой CalendarNode и разумеется как следствие другой LeagueMatchdayNode (АПЛ1 -> АПЛ3), т.е.поменялся matchDay:
        // 3.1 Помимо matchDay изменились любые параметры кроме user -> проверяем лимит для этого юзера в новой LeagueMatchdayNode (АПЛ3) для CalendarNode2
        // Если проверка пройдена - добавляем ставку в новую LeagueMatchdayNode(АПЛ3) в CalendarNode2 и удаляем старую ставку из LeagueMatchdayNode(АПЛ1) в CalendarNode1 для этого user.
        // 3.2 Изменился user -> проверяем лимит для нового юзера в новой LeagueMatchdayNode (АПЛ3) для CalendarNode2
        // Если проверка пройдена - добавляем для нового user ставку в LeagueMatchdayNode(АПЛ3) в CalendarNode2 и удаляем старую ставку из LeagueMatchdayNode(АПЛ1) в CalendarNode1 для предыдущего user.

        if (sameMatchDay && sameUser) {
            return; // 1.1 Никаких изменений или проверок, так как изменения при редактировании не касаются CalendarNode
        }
        // далее - все случаи кроме 1.1
        deleteBetFromCalendar(prevBet, prevCalendarNodeId);  // Удаление старой ставки из LeagueMatchdayNode
        addBetToCalendarNode(newBet, newCalendarNodeId, newBet.getLeague().getId(), newMatchDay); // checkLeagueBetLimit и добавление новой ставки в LeagueMatchdayNode
        newBet.setCalendarNodeId(newCalendarNodeId);  // Обновление ссылки на ID календаря
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

    public LeagueMatchdayNode getLeagueMatchdayNodeInSeason(String seasonId, String leagueId, String matchday) {
        return getEntityService.getLeagueMatchdayNodeFromSeasonOrThrow(seasonId, leagueId, matchday);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    private void saveCalendarNode(CalendarNode calendarNode) {
        calendarsRepository.save(calendarNode);
    }

    // ------------------------------------------------------------------------------------------------------ //


}
