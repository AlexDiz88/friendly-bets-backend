package net.friendly_bets.services;

import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameweekStatsServicePartialRecalcTest {

    @Mock
    CalendarsRepository calendarsRepository;
    @Mock
    BetsRepository betsRepository;
    @Mock
    GetEntityService getEntityService;

    @InjectMocks
    GameweekStatsService gameweekStatsService;

    @Test
    @DisplayName("Partial recalc processes nodes from earliest affected startDate to end")
    void recalculateFromEarliest_onlyTailOfSeason() {
        CalendarNode gw1 = node("gw1", LocalDate.of(2025, 8, 1));
        CalendarNode gw2 = node("gw2", LocalDate.of(2025, 8, 8));
        CalendarNode gw3 = node("gw3", LocalDate.of(2025, 8, 15));

        when(getEntityService.getListOfCalendarNodesWithBetsBySeasonOrThrow("season-1"))
                .thenReturn(List.of(gw3, gw1, gw2));
        when(getEntityService.getCalendarNodeOrThrow("gw3")).thenReturn(gw3);
        when(getEntityService.getSeasonOrThrow("season-1")).thenReturn(
                Season.builder().id("season-1").players(List.of()).build()
        );
        when(getEntityService.getListOfCalendarNodesIsFinishedOrThrow("season-1")).thenReturn(List.of());
        when(betsRepository.findAllByCalendarNodeId(anyString())).thenReturn(List.of());
        when(calendarsRepository.save(any(CalendarNode.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = gameweekStatsService.recalculateGameweekStatsFromEarliest("season-1", Set.of("gw3"));

        assertEquals(1, count);
        verify(getEntityService, times(1)).getCalendarNodeOrThrow("gw3");
        verify(getEntityService, times(0)).getCalendarNodeOrThrow("gw1");
        verify(getEntityService, times(0)).getCalendarNodeOrThrow("gw2");
    }

    private static CalendarNode node(String id, LocalDate start) {
        return CalendarNode.builder()
                .id(id)
                .seasonId("season-1")
                .startDate(start)
                .leagueMatchdayNodes(List.of())
                .build();
    }
}
