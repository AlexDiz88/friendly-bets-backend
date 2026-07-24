package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.TournamentFormatExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Текущий слот для страницы результатов: League.currentMatchDay, иначе первый незавершённый слот по match_schedules.
 */
@Component
@RequiredArgsConstructor
public class GameResultsCurrentSlotResolver {

    private final TournamentFormatExpander tournamentFormatExpander;
    private final MatchdaySlotSupport matchdaySlotSupport;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchScheduleQueryService matchScheduleQueryService;

    public int resolveCurrentSlotOrder(League league, TournamentFormat format, String season) {
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);
        if (slots.isEmpty()) {
            return 1;
        }

        return matchdaySlotSupport.resolveSlotOrder(league, league.getCurrentMatchDay())
                .orElseGet(() -> resolveFromSchedules(league, slots, season));
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
            if (records.isEmpty() || records.stream().anyMatch(m -> !MatchScheduleDisplayService.isFinalized(m))) {
                return slot.getOrder();
            }
        }
        return lastOrder;
    }
}
