package net.friendly_bets.matchschedule;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.TournamentFormatExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Игровой текущий слот: первый тур формата, в котором ещё не все матчи терминальны
 * ({@link MatchStatuses#isTerminal}). Источник — {@code match_schedules}, не
 * {@link League#getCurrentMatchDay()} (тот слот — прогресс ставок).
 */
@Component
@RequiredArgsConstructor
public class MatchScheduleCurrentSlotResolver {

    private final TournamentFormatExpander tournamentFormatExpander;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchScheduleQueryService matchScheduleQueryService;

    public int resolveCurrentSlotOrder(League league, TournamentFormat format, String season) {
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);
        if (slots.isEmpty()) {
            return 1;
        }
        return resolveFromSchedules(league, slots, season);
    }

    private int resolveFromSchedules(League league, List<ExpandedMatchdaySlot> slots, String seasonYear) {
        int lastOrder = slots.get(slots.size() - 1).getOrder();
        Season seasonEntity;
        try {
            seasonEntity = matchScheduleQueryService.resolveSeason(seasonYear);
        } catch (Exception e) {
            return 1;
        }
        String seasonId = seasonEntity.getId();
        String leagueId = league.getId();

        for (ExpandedMatchdaySlot slot : slots) {
            List<MatchSchedule> records = matchScheduleRepository
                    .findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                            leagueId, seasonId, slot.getOrder());
            if (isSlotStillOpen(records)) {
                return slot.getOrder();
            }
        }
        return lastOrder;
    }

    /**
     * Тур ещё идёт: нет расписания или есть матч не в терминальном статусе.
     * Пустой слот не пропускаем — это следующий (ещё не синканутый) тур.
     */
    private static boolean isSlotStillOpen(List<MatchSchedule> records) {
        if (records.isEmpty()) {
            return true;
        }
        return records.stream().anyMatch(m -> !MatchStatuses.isTerminal(m.getStatus()));
    }
}
