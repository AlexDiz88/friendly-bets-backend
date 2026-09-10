package net.friendly_bets.matchschedule;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.GetEntityService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Текущий игровой тур (calendar node) для плашки ставок / дефолта «По турам».
 * <p>
 * Primary: по {@code match_schedules} — первый узел, где ещё есть незавершённые матчи;
 * после полного терминала всех слотов — следующий узел по {@code startDate}.
 * Fallback: окно дат {@code startDate}/{@code endDate} (якорь = вчера), иначе ближайший по startDate.
 */
@Component
@RequiredArgsConstructor
public class CalendarGameweekCurrentResolver {

    enum MatchCoverage {
        /** Есть матч не в терминальном статусе. */
        OPEN,
        /** У всех слотов есть расписание и все матчи терминальны. */
        COMPLETE,
        /** Нет полной картины по schedules (пустой слот / не резолвится matchDay). */
        UNKNOWN
    }

    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchdaySlotSupport matchdaySlotSupport;
    private final GetEntityService getEntityService;

    public Optional<CalendarNode> resolve(String seasonId, List<CalendarNode> calendarNodes) {
        if (calendarNodes == null || calendarNodes.isEmpty()) {
            return Optional.empty();
        }

        List<CalendarNode> byStart = calendarNodes.stream()
                .sorted(Comparator.comparing(CalendarNode::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        CalendarNode lastComplete = null;
        for (CalendarNode node : byStart) {
            MatchCoverage coverage = evaluate(seasonId, node);
            if (coverage == MatchCoverage.OPEN) {
                return Optional.of(node);
            }
            if (coverage == MatchCoverage.COMPLETE) {
                lastComplete = node;
            }
        }

        if (lastComplete != null && lastComplete.getStartDate() != null) {
            LocalDate after = lastComplete.getStartDate();
            for (CalendarNode node : byStart) {
                if (node.getStartDate() != null && node.getStartDate().isAfter(after)) {
                    return Optional.of(node);
                }
            }
        }

        return Optional.of(pickByDates(byStart));
    }

    /**
     * Слот лиги для дефолта страницы «Результаты» внутри текущего gameweek:
     * первый с незавершёнными матчами; иначе первый без полной картины schedules;
     * иначе первый слот узла.
     */
    public Optional<LeagueMatchdayNode> pickResultsDefaultSlot(String seasonId, CalendarNode node) {
        if (node == null || node.getLeagueMatchdayNodes() == null || node.getLeagueMatchdayNodes().isEmpty()) {
            return Optional.empty();
        }
        List<LeagueMatchdayNode> slots = node.getLeagueMatchdayNodes();
        LeagueMatchdayNode firstUnknown = null;
        for (LeagueMatchdayNode slot : slots) {
            MatchCoverage coverage = evaluateSlot(seasonId, slot);
            if (coverage == MatchCoverage.OPEN) {
                return Optional.of(slot);
            }
            if (coverage == MatchCoverage.UNKNOWN && firstUnknown == null) {
                firstUnknown = slot;
            }
        }
        if (firstUnknown != null) {
            return Optional.of(firstUnknown);
        }
        return Optional.of(slots.get(0));
    }

    MatchCoverage evaluate(String seasonId, CalendarNode node) {
        List<LeagueMatchdayNode> slots = node.getLeagueMatchdayNodes();
        if (slots == null || slots.isEmpty()) {
            return MatchCoverage.UNKNOWN;
        }

        boolean anyNonTerminal = false;
        boolean allSlotsHaveData = true;

        for (LeagueMatchdayNode slot : slots) {
            MatchCoverage slotCoverage = evaluateSlot(seasonId, slot);
            if (slotCoverage == MatchCoverage.OPEN) {
                anyNonTerminal = true;
            } else if (slotCoverage != MatchCoverage.COMPLETE) {
                allSlotsHaveData = false;
            }
        }

        if (anyNonTerminal) {
            return MatchCoverage.OPEN;
        }
        if (allSlotsHaveData) {
            return MatchCoverage.COMPLETE;
        }
        return MatchCoverage.UNKNOWN;
    }

    MatchCoverage evaluateSlot(String seasonId, LeagueMatchdayNode slot) {
        if (slot == null || slot.getLeagueId() == null
                || slot.getMatchDay() == null || slot.getMatchDay().isBlank()) {
            return MatchCoverage.UNKNOWN;
        }
        Optional<Integer> order;
        try {
            League league = getEntityService.getLeagueOrThrow(slot.getLeagueId());
            order = matchdaySlotSupport.resolveSlotOrder(league, slot.getMatchDay().trim());
        } catch (Exception e) {
            return MatchCoverage.UNKNOWN;
        }
        if (order.isEmpty()) {
            return MatchCoverage.UNKNOWN;
        }

        List<MatchSchedule> matches = matchScheduleRepository
                .findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                        slot.getLeagueId(), seasonId, order.get());
        if (matches.isEmpty()) {
            return MatchCoverage.UNKNOWN;
        }
        if (matches.stream().anyMatch(m -> !MatchStatuses.isTerminal(m.getStatus()))) {
            return MatchCoverage.OPEN;
        }
        return MatchCoverage.COMPLETE;
    }

    /**
     * Запасной якорь по датам (как прежний фронтовый {@code pickDefaultCalendarNode}):
     * вчера строго внутри start/end, иначе ближайший startDate.
     */
    static CalendarNode pickByDates(List<CalendarNode> nodes) {
        LocalDate anchor = LocalDate.now().minusDays(1);

        Optional<CalendarNode> active = nodes.stream()
                .filter(n -> n.getStartDate() != null && n.getEndDate() != null)
                .filter(n -> anchor.isAfter(n.getStartDate()) && anchor.isBefore(n.getEndDate()))
                .findFirst();
        if (active.isPresent()) {
            return active.get();
        }

        return nodes.stream()
                .min(Comparator.comparingLong(n -> {
                    if (n.getStartDate() == null) {
                        return Long.MAX_VALUE;
                    }
                    return Math.abs(ChronoUnit.DAYS.between(anchor, n.getStartDate()));
                }))
                .orElse(nodes.get(0));
    }
}
