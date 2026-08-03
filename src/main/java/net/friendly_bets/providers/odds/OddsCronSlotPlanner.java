package net.friendly_bets.providers.odds;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.odds.OddsService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer ODDS cron policy: choose CURRENT / NEXT / skip from kickoff distance and odds coverage.
 * Manual admin sync does not use this planner.
 */
@Service
@RequiredArgsConstructor
public class OddsCronSlotPlanner {

    private static final Logger log = LoggerFactory.getLogger(OddsCronSlotPlanner.class);

    private final MatchScheduleQueryService matchScheduleQueryService;
    private final OddsService oddsService;
    private final ExternalDataLayerConfigService layerConfigService;

    public OddsCronSlotPlan plan(League league, String season, ExternalCompetitionInfoDto info, Instant now) {
        if (league == null || league.getLeagueCode() == null || info == null || now == null) {
            return OddsCronSlotPlan.skip("invalidInput");
        }
        int refreshWithinHours = layerConfigService.oddsRefreshWithinHours();
        List<Integer> window = OddsSlotWindow.resolveSlotOrders(info, OddsSlotScope.BOTH);
        if (window.isEmpty()) {
            return OddsCronSlotPlan.skip("noSlots");
        }

        int currentOrder = window.get(0);
        Integer nextOrder = window.size() > 1 ? window.get(1) : null;
        String leagueCode = league.getLeagueCode().name();

        List<MatchSchedule> currentAll = loadMatches(league, season, currentOrder);
        if (currentAll.isEmpty()) {
            return OddsCronSlotPlan.skip("noCurrentMatches");
        }

        List<MatchSchedule> currentPending = pendingOf(currentAll, now);
        if (currentPending.isEmpty()) {
            return planAfterCurrentExhausted(league, season, nextOrder, now);
        }

        Instant earliest = earliestKickoff(currentPending);
        if (earliest == null) {
            log.warn(
                    "odds cron: current pending without utc_kickoff league={} matchday={} pending={} — CURRENT MISSING_ONLY, skip next",
                    leagueCode,
                    currentOrder,
                    currentPending.size()
            );
            return OddsCronSlotPlan.sync(
                    OddsSlotScope.CURRENT,
                    List.of(currentOrder),
                    OddsFetchPolicy.MISSING_ONLY,
                    "currentMissingKickoff"
            );
        }

        Duration until = Duration.between(now, earliest);
        boolean near = until.isNegative()
                || until.compareTo(Duration.ofHours(refreshWithinHours)) <= 0;

        if (near) {
            return OddsCronSlotPlan.sync(
                    OddsSlotScope.CURRENT,
                    List.of(currentOrder),
                    OddsFetchPolicy.FORCE,
                    "nearForceCurrent"
            );
        }

        if (!allHaveOdds(currentPending)) {
            return OddsCronSlotPlan.sync(
                    OddsSlotScope.CURRENT,
                    List.of(currentOrder),
                    OddsFetchPolicy.REFRESH_WINDOW,
                    "farFillCurrent"
            );
        }

        if (nextOrder == null) {
            return OddsCronSlotPlan.skip("farCurrentCompleteNoNext");
        }

        List<MatchSchedule> nextPending = pendingOf(loadMatches(league, season, nextOrder), now);
        if (nextPending.isEmpty() || allHaveOdds(nextPending)) {
            return OddsCronSlotPlan.skip("farBothComplete");
        }

        return OddsCronSlotPlan.sync(
                OddsSlotScope.NEXT,
                List.of(nextOrder),
                OddsFetchPolicy.REFRESH_WINDOW,
                "farFillNext"
        );
    }

    private OddsCronSlotPlan planAfterCurrentExhausted(
            League league,
            String season,
            Integer nextOrder,
            Instant now
    ) {
        if (nextOrder == null) {
            return OddsCronSlotPlan.skip("currentExhaustedNoNext");
        }
        List<MatchSchedule> nextPending = pendingOf(loadMatches(league, season, nextOrder), now);
        if (nextPending.isEmpty() || allHaveOdds(nextPending)) {
            return OddsCronSlotPlan.skip("currentExhaustedNextComplete");
        }
        return OddsCronSlotPlan.sync(
                OddsSlotScope.NEXT,
                List.of(nextOrder),
                OddsFetchPolicy.REFRESH_WINDOW,
                "currentExhaustedFillNext"
        );
    }

    private List<MatchSchedule> loadMatches(League league, String season, int matchday) {
        return matchScheduleQueryService.getMatches(
                league.getLeagueCode().name(),
                matchday,
                season,
                league.getId()
        );
    }

    private static List<MatchSchedule> pendingOf(List<MatchSchedule> matches, Instant now) {
        List<MatchSchedule> pending = new ArrayList<>();
        for (MatchSchedule match : matches) {
            if (match == null) {
                continue;
            }
            if (MatchScheduleDisplayService.isFinalized(match)) {
                continue;
            }
            if (MatchScheduleNotStarted.isNotStarted(match, now)) {
                pending.add(match);
            }
        }
        return pending;
    }

    private static Instant earliestKickoff(List<MatchSchedule> pending) {
        Instant earliest = null;
        for (MatchSchedule match : pending) {
            Instant kickoff = match.getUtcKickoff();
            if (kickoff == null) {
                continue;
            }
            if (earliest == null || kickoff.isBefore(earliest)) {
                earliest = kickoff;
            }
        }
        return earliest;
    }

    private boolean allHaveOdds(List<MatchSchedule> pending) {
        for (MatchSchedule match : pending) {
            if (!hasOdds(match)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasOdds(MatchSchedule match) {
        if (match.getId() == null) {
            return false;
        }
        return oddsService.findByMatchScheduleId(match.getId())
                .map(odds -> odds.getMarketGroups() != null && !odds.getMarketGroups().isEmpty())
                .orElse(false);
    }
}
