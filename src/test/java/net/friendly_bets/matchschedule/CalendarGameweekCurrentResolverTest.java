package net.friendly_bets.matchschedule;

import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.GetEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarGameweekCurrentResolverTest {

    private static final String SEASON_ID = "season-1";
    private static final String EPL_ID = "epl";
    private static final String BL_ID = "bl";

    @Mock
    MatchScheduleRepository matchScheduleRepository;
    @Mock
    MatchdaySlotSupport matchdaySlotSupport;
    @Mock
    GetEntityService getEntityService;

    @InjectMocks
    CalendarGameweekCurrentResolver resolver;

    @Test
    void staysOnOpenGameweekWhileAnyMatchNotTerminal() {
        CalendarNode gw1 = node("gw1", LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 31), eplBl("1"));
        CalendarNode gw2 = node("gw2", LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 7), eplBl("2"));
        stubLeague(EPL_ID);
        stubLeague(BL_ID);
        stubOrder("1", 1);
        stubMatches(EPL_ID, 1, List.of(match("FINISHED"), match("SCHEDULED")));
        stubMatches(BL_ID, 1, List.of(match("FINISHED")));

        Optional<CalendarNode> result = resolver.resolve(SEASON_ID, List.of(gw2, gw1));

        assertTrue(result.isPresent());
        assertEquals("gw1", result.get().getId());
    }

    @Test
    void advancesToNextNodeWhenAllSlotsTerminal() {
        CalendarNode gw1 = node("gw1", LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 31), eplBl("1"));
        CalendarNode gw2 = node("gw2", LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 7), eplBl("2"));
        stubLeague(EPL_ID);
        stubLeague(BL_ID);
        stubOrder("1", 1);
        stubMatches(EPL_ID, 1, List.of(match("FINISHED"), match("AWARDED")));
        stubMatches(BL_ID, 1, List.of(match("CANCELED")));

        Optional<CalendarNode> result = resolver.resolve(SEASON_ID, List.of(gw1, gw2));

        assertTrue(result.isPresent());
        assertEquals("gw2", result.get().getId());
    }

    @Test
    void emptySchedulesFallBackToDateWindow() {
        CalendarNode gw1 = node("gw1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4), eplBl("1"));
        CalendarNode gw2 = node("gw2", LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 11), eplBl("2"));
        stubLeague(EPL_ID);
        stubLeague(BL_ID);
        stubOrder("1", 1);
        stubOrder("2", 2);
        stubMatches(EPL_ID, 1, List.of());
        stubMatches(BL_ID, 1, List.of());
        stubMatches(EPL_ID, 2, List.of());
        stubMatches(BL_ID, 2, List.of());

        CalendarNode picked = CalendarGameweekCurrentResolver.pickByDates(List.of(gw1, gw2));
        assertEquals(
                picked.getId(),
                resolver.resolve(SEASON_ID, List.of(gw1, gw2)).orElseThrow().getId()
        );
    }

    @Test
    void pickByDatesPrefersActiveWindowUsingYesterdayAnchor() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CalendarNode active = node(
                "active",
                yesterday.minusDays(2),
                yesterday.plusDays(2),
                eplBl("1")
        );
        CalendarNode other = node(
                "other",
                yesterday.plusDays(10),
                yesterday.plusDays(13),
                eplBl("2")
        );

        assertEquals("active", CalendarGameweekCurrentResolver.pickByDates(List.of(other, active)).getId());
    }

    private void stubLeague(String leagueId) {
        when(getEntityService.getLeagueOrThrow(leagueId))
                .thenReturn(League.builder().id(leagueId).build());
    }

    private void stubOrder(String matchDay, int order) {
        when(matchdaySlotSupport.resolveSlotOrder(any(League.class), eq(matchDay)))
                .thenReturn(Optional.of(order));
    }

    private void stubMatches(String leagueId, int matchday, List<MatchSchedule> matches) {
        when(matchScheduleRepository.findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                eq(leagueId), eq(SEASON_ID), eq(matchday)))
                .thenReturn(matches);
    }

    private static List<LeagueMatchdayNode> eplBl(String matchDay) {
        return List.of(
                LeagueMatchdayNode.builder().leagueId(EPL_ID).matchDay(matchDay).build(),
                LeagueMatchdayNode.builder().leagueId(BL_ID).matchDay(matchDay).build()
        );
    }

    private static CalendarNode node(
            String id,
            LocalDate start,
            LocalDate end,
            List<LeagueMatchdayNode> slots
    ) {
        return CalendarNode.builder()
                .id(id)
                .seasonId(SEASON_ID)
                .startDate(start)
                .endDate(end)
                .leagueMatchdayNodes(slots)
                .build();
    }

    private static MatchSchedule match(String status) {
        return MatchSchedule.builder().status(status).build();
    }
}
