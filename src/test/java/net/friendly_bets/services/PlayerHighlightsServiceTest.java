package net.friendly_bets.services;

import net.friendly_bets.dto.PlayerHighlightDto;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.GameweekStats;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;
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
    @DisplayName("Aggregates form, best bet, streaks, best gameweek and per-league teams")
    void draft_computesPlayerMetrics() {
        Team arsenal = Team.builder().id("t-ars").title("Arsenal").logo("arsenal").build();
        Team chelsea = Team.builder().id("t-che").title("Chelsea").logo("chelsea").build();
        Instant t0 = Instant.parse("2025-08-01T12:00:00Z");
        List<PlayerHighlightsService.HighlightBetRow> bets = List.of(
                row("u1", Bet.BetStatus.WON, 10.0, 2.0, t0, "t-ars", "t-che", "epl", "1", 10),
                row("u1", Bet.BetStatus.WON, 25.5, 3.1, t0.plusSeconds(60), "t-ars", "t-che", "epl", "2", 10),
                row("u1", Bet.BetStatus.LOST, -10.0, 1.8, t0.plusSeconds(120), "t-che", "t-ars", "epl", "3", 10),
                row("u1", Bet.BetStatus.LOST, -10.0, 1.8, t0.plusSeconds(150), "t-che", "t-ars", "bl", "3", 10),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(180), "t-ars", "t-che", "epl", "4", 10),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(240), "t-ars", "t-che", "epl", "5", 10),
                row("u1", Bet.BetStatus.WON, 5.0, 1.5, t0.plusSeconds(300), "t-ars", "t-che", "epl", "6", 10),
                row("u1", Bet.BetStatus.RETURNED, 0.0, 2.0, t0.plusSeconds(360), "t-ars", "t-che", "epl", "7", 10)
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
                .leagueMatchdayNodes(List.of(
                        LeagueMatchdayNode.builder().leagueCode(League.LeagueCode.BL).matchDay("1").build(),
                        LeagueMatchdayNode.builder().leagueCode(League.LeagueCode.EPL).matchDay("2").build()
                ))
                .gameweekStats(List.of(GameweekStats.builder().userId("u1").balanceChange(21.0).build()))
                .build();

        Map<String, Map<String, Double>> teamBalancesByLeague = new HashMap<>();
        teamBalancesByLeague.put("epl", Map.of("t-ars", 40.0, "t-che", -12.0));
        teamBalancesByLeague.put("bl", Map.of("t-ars", 5.0));
        Map<String, String> leagueCodeById = Map.of("epl", "EPL", "bl", "BL");
        Map<String, Team> teamsById = Map.of("t-ars", arsenal, "t-che", chelsea);

        PlayerHighlightDto highlight = PlayerHighlightsService.toDto(
                PlayerHighlightsService.draftPlayerHighlight(
                        "u1",
                        bets,
                        List.of(gw1, gw2),
                        teamBalancesByLeague,
                        leagueCodeById
                ),
                teamsById
        );

        assertEquals("u1", highlight.getUserId());
        assertEquals(
                List.of("WON", "WON", "LOST", "LOST", "WON", "WON", "WON", "RETURNED"),
                highlight.getRecentForm()
        );
        assertEquals(3, highlight.getBestWinStreak());
        assertEquals(2, highlight.getWorstLoseStreak());
        assertNotNull(highlight.getBiggestWin());
        assertEquals(25.5, highlight.getBiggestWin().getBalanceChange());
        assertEquals(3.1, highlight.getBiggestWin().getBetOdds());
        assertEquals(10, highlight.getBiggestWin().getBetSize());
        assertEquals("EPL", highlight.getBiggestWin().getLeagueCode());
        assertEquals("2", highlight.getBiggestWin().getMatchDay());
        assertEquals("t-ars", highlight.getBiggestWin().getHomeTeam().getId());
        assertEquals("arsenal", highlight.getBiggestWin().getHomeTeam().getLogoKey());
        assertNotNull(highlight.getBestGameweek());
        assertEquals("gw2", highlight.getBestGameweek().getCalendarNodeId());
        assertEquals(21.0, highlight.getBestGameweek().getBalanceChange());
        assertEquals(2, highlight.getBestGameweek().getMatchdays().size());
        assertEquals("EPL", highlight.getBestGameweek().getMatchdays().get(0).getLeagueCode());
        assertEquals("2", highlight.getBestGameweek().getMatchdays().get(0).getMatchDay());
        assertEquals("BL", highlight.getBestGameweek().getMatchdays().get(1).getLeagueCode());
        assertEquals(2, highlight.getLeagueTeams().size());
        assertEquals("EPL", highlight.getLeagueTeams().get(0).getLeagueCode());
        assertEquals("t-ars", highlight.getLeagueTeams().get(0).getBest().getId());
        assertEquals(40.0, highlight.getLeagueTeams().get(0).getBest().getActualBalance());
        assertEquals("t-che", highlight.getLeagueTeams().get(0).getWorst().getId());
        assertEquals(-12.0, highlight.getLeagueTeams().get(0).getWorst().getActualBalance());
        assertEquals("BL", highlight.getLeagueTeams().get(1).getLeagueCode());
        assertEquals("t-ars", highlight.getLeagueTeams().get(1).getBest().getId());
        assertEquals("t-ars", highlight.getLeagueTeams().get(1).getWorst().getId());
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
                    null,
                    null,
                    null,
                    null
            ));
        }

        PlayerHighlightDto highlight = PlayerHighlightsService.toDto(
                PlayerHighlightsService.draftPlayerHighlight("u1", bets, List.of(), Map.of(), Map.of()),
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
            String awayId,
            String leagueId,
            String matchDay,
            Integer betSize
    ) {
        return PlayerHighlightsService.HighlightBetRow.builder()
                .userId(userId)
                .status(status)
                .balanceChange(change)
                .betOdds(odds)
                .resultAt(at)
                .homeTeamId(homeId)
                .awayTeamId(awayId)
                .leagueId(leagueId)
                .matchDay(matchDay)
                .betSize(betSize)
                .build();
    }
}
