package net.friendly_bets.matchschedule;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.TournamentFormatExpander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchScheduleCurrentSlotResolverTest {

    private static final String LEAGUE_ID = "epl";
    private static final String SEASON_ID = "season-1";
    private static final String SEASON_YEAR = "2025";

    @Mock
    TournamentFormatExpander tournamentFormatExpander;
    @Mock
    MatchScheduleRepository matchScheduleRepository;
    @Mock
    MatchScheduleQueryService matchScheduleQueryService;

    @InjectMocks
    MatchScheduleCurrentSlotResolver resolver;

    private final TournamentFormat format = TournamentFormat.builder().id("fmt").build();
    private final League league = League.builder()
            .id(LEAGUE_ID)
            .currentMatchDay("2")
            .build();
    private final Season season = Season.builder().id(SEASON_ID).build();

    @BeforeEach
    void setUp() {
        when(tournamentFormatExpander.expand(format)).thenReturn(List.of(slot(1), slot(2), slot(3)));
    }

    @Test
    void staysOnOpenMatchdayEvenIfLeagueCurrentMatchDayAlreadyAdvanced() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR)).thenReturn(season);
        stubMatchday(1, List.of(match("FINISHED"), match("SCHEDULED")));

        assertEquals(1, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    @Test
    void advancesOnlyWhenEveryMatchOnSlotIsTerminal() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR)).thenReturn(season);
        stubMatchday(1, List.of(match("FINISHED"), match("CANCELED"), match("AWARDED")));
        stubMatchday(2, List.of(match("SCHEDULED")));

        assertEquals(2, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    @Test
    void liveMatchKeepsSlotOpen() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR)).thenReturn(season);
        stubMatchday(1, List.of(match("FINISHED"), match("IN_PLAY")));

        assertEquals(1, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    @Test
    void emptyNextSlotIsCurrentAfterPreviousCompleted() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR)).thenReturn(season);
        stubMatchday(1, List.of(match("FINISHED")));
        stubMatchday(2, List.of());

        assertEquals(2, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    @Test
    void lastSlotWhenWholeFormatIsComplete() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR)).thenReturn(season);
        stubMatchday(1, List.of(match("FINISHED")));
        stubMatchday(2, List.of(match("FINISHED")));
        stubMatchday(3, List.of(match("FINISHED")));

        assertEquals(3, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    @Test
    void seasonResolveFailureFallsBackToFirstSlot() {
        when(matchScheduleQueryService.resolveSeason(SEASON_YEAR))
                .thenThrow(new BadRequestException("noActiveSeasonWasFounded"));

        assertEquals(1, resolver.resolveCurrentSlotOrder(league, format, SEASON_YEAR));
    }

    private void stubMatchday(int order, List<MatchSchedule> records) {
        when(matchScheduleRepository.findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                eq(LEAGUE_ID), eq(SEASON_ID), eq(order)))
                .thenReturn(records);
    }

    private static ExpandedMatchdaySlot slot(int order) {
        return ExpandedMatchdaySlot.builder()
                .id(String.valueOf(order))
                .order(order)
                .kind(ExpandedMatchdaySlot.Kind.REGULAR)
                .labelKey(String.valueOf(order))
                .build();
    }

    private static MatchSchedule match(String status) {
        return MatchSchedule.builder().status(status).build();
    }
}
