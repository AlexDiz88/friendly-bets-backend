package net.friendly_bets.providers.odds;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.OddsService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.MatchScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsCronSlotPlannerTest {

    @Mock
    MatchScheduleQueryService matchScheduleQueryService;
    @Mock
    OddsService oddsService;
    @Mock
    ExternalDataLayerConfigService layerConfigService;

    @InjectMocks
    OddsCronSlotPlanner planner;

    private final League league = League.builder()
            .id("bl")
            .leagueCode(League.LeagueCode.BL)
            .build();
    private final Instant now = Instant.parse("2026-08-03T10:00:00Z");

    @BeforeEach
    void setUp() {
        when(layerConfigService.oddsRefreshWithinHours()).thenReturn(36);
    }

    @Test
    void far_missingCurrentOdds_onlyCurrentRefreshWindow() {
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(1), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("c1", now.plus(5, ChronoUnit.DAYS))));
        when(oddsService.findByMatchScheduleId("c1")).thenReturn(Optional.empty());

        OddsCronSlotPlan plan = planner.plan(league, "2026", info(1), now);

        assertFalse(plan.skip());
        assertEquals(OddsSlotScope.CURRENT, plan.scope());
        assertEquals(List.of(1), plan.slotOrders());
        assertEquals(OddsFetchPolicy.REFRESH_WINDOW, plan.fetchPolicy());
        assertEquals("farFillCurrent", plan.reason());
    }

    @Test
    void far_currentComplete_nextMissing_onlyNext() {
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(1), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("c1", now.plus(5, ChronoUnit.DAYS))));
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(2), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("n1", now.plus(12, ChronoUnit.DAYS))));
        when(oddsService.findByMatchScheduleId("c1")).thenReturn(Optional.of(oddsDoc()));
        when(oddsService.findByMatchScheduleId("n1")).thenReturn(Optional.empty());

        OddsCronSlotPlan plan = planner.plan(league, "2026", info(1), now);

        assertFalse(plan.skip());
        assertEquals(OddsSlotScope.NEXT, plan.scope());
        assertEquals(List.of(2), plan.slotOrders());
        assertEquals(OddsFetchPolicy.REFRESH_WINDOW, plan.fetchPolicy());
        assertEquals("farFillNext", plan.reason());
    }

    @Test
    void far_bothComplete_skip() {
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(1), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("c1", now.plus(5, ChronoUnit.DAYS))));
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(2), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("n1", now.plus(12, ChronoUnit.DAYS))));
        when(oddsService.findByMatchScheduleId(any())).thenReturn(Optional.of(oddsDoc()));

        OddsCronSlotPlan plan = planner.plan(league, "2026", info(1), now);

        assertTrue(plan.skip());
        assertEquals("farBothComplete", plan.reason());
    }

    @Test
    void near_forceCurrentOnly() {
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(1), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("c1", now.plus(10, ChronoUnit.HOURS))));

        OddsCronSlotPlan plan = planner.plan(league, "2026", info(1), now);

        assertFalse(plan.skip());
        assertEquals(OddsSlotScope.CURRENT, plan.scope());
        assertEquals(List.of(1), plan.slotOrders());
        assertEquals(OddsFetchPolicy.FORCE, plan.fetchPolicy());
        assertEquals("nearForceCurrent", plan.reason());
    }

    @Test
    void missingKickoff_currentMissingOnly_skipNext() {
        when(matchScheduleQueryService.getMatches(eq("BL"), eq(1), eq("2026"), eq("bl")))
                .thenReturn(List.of(pending("c1", null)));

        OddsCronSlotPlan plan = planner.plan(league, "2026", info(1), now);

        assertFalse(plan.skip());
        assertEquals(OddsSlotScope.CURRENT, plan.scope());
        assertEquals(OddsFetchPolicy.MISSING_ONLY, plan.fetchPolicy());
        assertEquals("currentMissingKickoff", plan.reason());
    }

    private static ExternalCompetitionInfoDto info(int current) {
        return ExternalCompetitionInfoDto.builder()
                .currentMatchday(current)
                .matchdayCount(34)
                .matchdaySlots(List.of(
                        ExternalMatchdaySlotDto.builder().value(1).label("1").build(),
                        ExternalMatchdaySlotDto.builder().value(2).label("2").build()
                ))
                .build();
    }

    private static MatchSchedule pending(String id, Instant kickoff) {
        return MatchSchedule.builder()
                .id(id)
                .status("SCHEDULED")
                .utcKickoff(kickoff)
                .build();
    }

    private static Odds oddsDoc() {
        return Odds.builder()
                .matchScheduleId("x")
                .marketGroups(List.of(OddsMarketGroup.builder().build()))
                .build();
    }
}
