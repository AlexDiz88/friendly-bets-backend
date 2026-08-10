package net.friendly_bets.ruscore;

import net.friendly_bets.providers.FullMatchStatusSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuscoreGameSummaryParserTest {

    private final RuscoreGameSummaryParser parser = new RuscoreGameSummaryParser();

    @Test
    void parsesFinishedMatchGoalsStatsAndAddedTime() throws IOException {
        String html = readFixture("ruscore/game-563678-summary-mini.html");
        RuscoreParsedFullMatch parsed = parser.parse(html, "563678", "fk-dinamo-moscow-fc-dynamo-makhachkala");

        assertTrue(FullMatchStatusSupport.isProviderFinished(parsed.getStatusText()));
        assertNotNull(parsed.getGameScore());
        assertEquals("3:1", parsed.getGameScore().getFullTime());
        assertNotNull(parsed.getGoals());
        assertTrue(parsed.getGoals().size() >= 3);
        assertTrue(parsed.getGoals().stream().noneMatch(g ->
                g.getPlayerName() != null && g.getPlayerName().toLowerCase().contains("засчитан")));
        assertTrue(parsed.getGoals().stream().anyMatch(g -> "HOME".equals(g.getTeamSide())));
        assertTrue(parsed.getGoals().stream().anyMatch(g -> g.getPlayerName() != null && !g.getPlayerName().isBlank()));

        assertNotNull(parsed.getStats());
        assertEquals(67, parsed.getStats().getPossessionHome());
        assertEquals(33, parsed.getStats().getPossessionAway());
        assertEquals(12, parsed.getStats().getShotsHome());
        assertEquals(7, parsed.getStats().getShotsAway());
        assertNotNull(parsed.getStats().getShotsOnTargetHome());
        assertNotNull(parsed.getStats().getCornersHome());
        assertNotNull(parsed.getStats().getOffsidesHome());
        assertNotNull(parsed.getStats().getSavesHome());
        assertNotNull(parsed.getStats().getYellowCardsHome());
        assertNotNull(parsed.getStats().getRedCardsHome());
        assertNull(parsed.getStats().getXgHome());

        assertEquals(1, parsed.getAddedTimeFirstHalf());
        assertEquals(4, parsed.getAddedTimeSecondHalf());
    }

    @Test
    void nuremberg_skipsVarAndMissedPenalty_htAndYellowCards() throws IOException {
        String html = readFixture("ruscore/game-574496-summary-mini.html");
        RuscoreParsedFullMatch parsed = parser.parse(
                html, "574496", "1-fc-nuremberg-dynamo-dresden");

        assertTrue(FullMatchStatusSupport.isProviderFinished(parsed.getStatusText()));
        assertEquals("3:0", parsed.getGameScore().getFullTime());
        assertEquals("2:0", parsed.getGameScore().getFirstTime());
        assertEquals(3, parsed.getGoals().size());
        assertTrue(parsed.getGoals().stream().noneMatch(g ->
                g.getPlayerName() != null && g.getPlayerName().toLowerCase().replace('ё', 'е').contains("засчитан")));
        assertEquals(0, parsed.getStats().getYellowCardsHome());
        assertEquals(2, parsed.getStats().getYellowCardsAway());
        assertEquals(3, parsed.getAddedTimeFirstHalf());
        assertEquals(5, parsed.getAddedTimeSecondHalf());
    }

    @Test
    void marksInMatchPenaltyFromScoreSubtype11() {
        // Bot HTML stores "11"; UI appends "-м" via CSS (::after).
        String html = """
                <html><body>
                <div data-test-id="status">завершён</div>
                <span data-test-id="overall">3 : 1</span>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="home-player">
                    <span class="_score_x">1 : 0</span>
                    <a data-test-id="player-name">Обычный Гол</a>
                  </div>
                  <span data-test-id="time">14'</span>
                </div>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="home-player">
                    <span class="_score_x">2 : 1 <span class="_scoreSubtype_x">11</span></span>
                    <a data-test-id="player-name">Т. Джигерджи</a>
                  </div>
                  <span data-test-id="time">71'</span>
                </div>
                </body></html>
                """;
        RuscoreParsedFullMatch parsed = parser.parse(html, "574501", "energie-cottbus-hannover-96");
        assertEquals(2, parsed.getGoals().size());
        assertNull(parsed.getGoals().get(0).getPenalty());
        assertEquals("71", parsed.getGoals().get(1).getMinute());
        assertEquals(Boolean.TRUE, parsed.getGoals().get(1).getPenalty());
    }

    @Test
    void marksOwnGoalFromScoreSubtypeA() {
        String html = """
                <html><body>
                <div data-test-id="status">завершён</div>
                <span data-test-id="overall">0 : 2</span>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="away-player">
                    <span class="_score_x">0 : 1</span>
                    <a data-test-id="player-name">M. Younis</a>
                  </div>
                  <span data-test-id="time">12'</span>
                </div>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="away-player">
                    <span class="_score_x">0 : 2 <span class="_scoreSubtype_x">A</span></span>
                    <a data-test-id="player-name">M. Kupijbida</a>
                  </div>
                  <span data-test-id="time">60'</span>
                </div>
                </body></html>
                """;
        RuscoreParsedFullMatch parsed = parser.parse(html, "544146", "ac-horsens-brondby");
        assertEquals(2, parsed.getGoals().size());
        assertNull(parsed.getGoals().get(0).getOwnGoal());
        assertEquals("60", parsed.getGoals().get(1).getMinute());
        assertEquals(Boolean.TRUE, parsed.getGoals().get(1).getOwnGoal());
        assertEquals("AWAY", parsed.getGoals().get(1).getTeamSide());
    }

    @Test
    void secondYellowBecomesRedEventAndStats() {
        String html = """
                <html><body>
                <div data-test-id="status">завершён</div>
                <span data-test-id="overall">2 : 1</span>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="away-player">
                    <figure><svg class="icon _iconCard_x"><g fill="none">
                      <rect fill="#FFD600"/><rect fill="#FF5050"/>
                    </g></svg></figure>
                    <a data-test-id="player-name">Д. Ито</a>
                  </div>
                  <span data-test-id="time">59'</span>
                </div>
                <div data-test-id="incident-wrapper">
                  <div data-test-id="away-player">
                    <span class="_score_x">0 : 1</span>
                    <a data-test-id="player-name">I. Ubandoma</a>
                  </div>
                  <span data-test-id="time">54'</span>
                </div>
                <div data-test-id="stat-item">
                  <span data-test-id="metric-name">Жёлтые карточки</span>
                  <span data-test-id="home-score">2</span>
                  <span data-test-id="away-score">2</span>
                </div>
                <div data-test-id="stat-item">
                  <span data-test-id="metric-name">Красные карточки</span>
                  <span data-test-id="home-score">0</span>
                  <span data-test-id="away-score">0</span>
                </div>
                <div data-test-id="stat-item">
                  <span data-test-id="metric-name">Вторые жёлтые карточки</span>
                  <span data-test-id="home-score">0</span>
                  <span data-test-id="away-score">1</span>
                </div>
                </body></html>
                """;
        RuscoreParsedFullMatch parsed = parser.parse(html, "559109", "rsc-anderlecht-raal-la-louviere");
        assertEquals(2, parsed.getGoals().size());
        var red = parsed.getGoals().stream().filter(g -> Boolean.TRUE.equals(g.getRedCard())).findFirst().orElseThrow();
        assertEquals("59", red.getMinute());
        assertEquals("AWAY", red.getTeamSide());
        assertEquals(Boolean.TRUE, red.getSecondYellow());
        assertEquals(0, parsed.getStats().getRedCardsHome());
        assertEquals(1, parsed.getStats().getRedCardsAway());
        assertEquals(2, parsed.getStats().getYellowCardsHome());
        assertEquals(2, parsed.getStats().getYellowCardsAway());
    }

    @Test
    void teamNamesFromCalendarMini() throws IOException {
        String html = readFixture("ruscore/epl-calendar-mini.html");
        var names = RuscoreTeamNamesService.parseTeamNamesFromCalendar(html);
        assertTrue(names.size() >= 10);
        assertTrue(names.stream().anyMatch(n -> n.contains("Арсенал") || n.toLowerCase().contains("arsenal")));
    }

    private static String readFixture(String path) throws IOException {
        try (var in = RuscoreGameSummaryParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
