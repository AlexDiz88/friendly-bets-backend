package net.friendly_bets.services;

import net.friendly_bets.dto.PlayerHighlightDto;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.GameweekStats;
import net.friendly_bets.models.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerHighlightsServiceTest {

    @Test
    @DisplayName("Aggregates form, biggest win, streak, best gameweek and team extremes")
    void draft_computesPlayerMetrics() {
        Team arsenal = Team.builder().id("t-ars").title("Arsenal").build();
        Team chelsea = Team.builder().id("t-che").title("Chelsea").build();
        Instant t0 = Instant.parse("2025-08-01T12:00:00Z");
        List<PlayerHighlightsService.HighlightBetRow> bets = List.of(
                row("u1", Bet.BetStatus.WON, 10.0, 2.0, t0, "t-ars", "t-che"),
                row("u1", Bet.BetStatus.WON, 25.5, 3.1, t0.plusSeconds(60), "t-ars", "t-che"),
                row("u1", Bet.BetStatus.LOST, -10.0, 1.8, t0.plusSeconds(120), "t-che", "t-ars"),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(180), "t-ars", "t-che"),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(240), "t-ars", "t-che"),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(300), "t-ars", "t-che"),
                row("u1", Bet.BetStatus.RETURNED, 0.0, 2.0, t0.plusSeconds(360), "t-ars", "t-che")
        );

        CalendarNode gw1 = CalendarNode.builder()
                .id("gw1")
                .isFinished(true)
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2025, 8, 4))
                .gameweekStats(List.of(GameweekStats.builder().userId("u1").balanceChange(8.0).build()))
                .build();
        CalendarNode gw2 = CalendarNode.builder()
                .id("gw2")
                .isFinished(true)
                .startDate(LocalDate.of(2025, 8, 8))
                .endDate(LocalDate.of(2025, 8, 11))
                .gameweekStats(List.of(GameweekStats.builder().userId("u1").balanceChange(21.0).build()))
                .build();

        Map<String, Double> teamBalances = new HashMap<>();
        teamBalances.put("t-ars", 40.0);
        teamBalances.put("t-che", -12.0);
        Map<String, Team> teamsById = Map.of("t-ars", arsenal, "t-che", chelsea);

        PlayerHighlightDto highlight = PlayerHighlightsService.toDto(
                PlayerHighlightsService.draftPlayerHighlight("u1", bets, List.of(gw1, gw2), teamBalances),
                teamsById
        );

        assertEquals("u1", highlight.getUserId());
        assertEquals(List.of("WON", "WON", "LOST", "WON", "WON", "WON", "RETURNED"), highlight.getRecentForm());
        assertEquals(3, highlight.getBestWinStreak());
        assertNotNull(highlight.getBiggestWin());
        assertEquals(25.5, highlight.getBiggestWin().getBalanceChange());
        assertEquals(3.1, highlight.getBiggestWin().getBetOdds());
        assertEquals("t-ars", highlight.getBiggestWin().getHomeTeam().getId());
        assertNotNull(highlight.getBestGameweek());
        assertEquals("gw2", highlight.getBestGameweek().getCalendarNodeId());
        assertEquals(21.0, highlight.getBestGameweek().getBalanceChange());
        assertEquals("t-ars", highlight.getMostProfitableTeam().getId());
        assertEquals(40.0, highlight.getMostProfitableTeam().getActualBalance());
        assertEquals("t-che", highlight.getMostUnprofitableTeam().getId());
        assertEquals(-12.0, highlight.getMostUnprofitableTeam().getActualBalance());
    }

    @Test
    @DisplayName("Recent form keeps only the last 12 completed bets")
    void draft_trimsFormToLastTwelve() {
        Instant t0 = Instant.parse("2025-08-01T12:00:00Z");
        List<PlayerHighlightsService.HighlightBetRow> bets = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            bets.add(row(
                    "u1",
                    i % 2 == 0 ? Bet.BetStatus.WON : Bet.BetStatus.LOST,
                    i % 2 == 0 ? 1.0 : -1.0,
                    1.5,
                    t0.plusSeconds(i),
                    null,
                    null
            ));
        }

        PlayerHighlightDto highlight = PlayerHighlightsService.toDto(
                PlayerHighlightsService.draftPlayerHighlight("u1", bets, List.of(), Map.of()),
                Map.of()
        );
        assertEquals(12, highlight.getRecentForm().size());
        assertEquals("LOST", highlight.getRecentForm().get(0));
        assertEquals("WON", highlight.getRecentForm().get(11));
    }

    private static PlayerHighlightsService.HighlightBetRow row(
            String userId,
            Bet.BetStatus status,
            Double change,
            Double odds,
            Instant at,
            String homeId,
            String awayId
    ) {
        return PlayerHighlightsService.HighlightBetRow.builder()
                .userId(userId)
                .status(status)
                .balanceChange(change)
                .betOdds(odds)
                .resultAt(at)
                .homeTeamId(homeId)
                .awayTeamId(awayId)
                .build();
    }
}
