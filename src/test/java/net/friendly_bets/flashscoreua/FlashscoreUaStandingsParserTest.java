package net.friendly_bets.flashscoreua;

import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreUaStandingsParserTest {

    private final FlashscoreUaStandingsParser parser = new FlashscoreUaStandingsParser();

    @Test
    void parseOverallTable_extractsRowsZonesAndLogos() throws IOException {
        String feed = Files.readString(
                Path.of("src/test/resources/flashscoreua/epl-standings.feed"),
                StandardCharsets.UTF_8
        );

        StandingsTableSnapshot snapshot = parser.parse(
                feed,
                "https://www.flashscore.com.ua/football/england/premier-league/#/CfoA8Dmm/standings/overall/"
        );

        assertEquals(3, snapshot.getRows().size());
        assertEquals(2, snapshot.getZoneRules().size());

        StandingRowSnapshot first = snapshot.getRows().get(0);
        assertEquals(1, first.getRank());
        assertEquals("Arsenal", first.getExternalTeamName());
        assertEquals(38, first.getPlayed());
        assertEquals(26, first.getWins());
        assertEquals(7, first.getDraws());
        assertEquals(5, first.getLosses());
        assertEquals(71, first.getGoalsFor());
        assertEquals(27, first.getGoalsAgainst());
        assertEquals(44, first.getGoalDifference());
        assertEquals(85, first.getPoints());
        assertEquals("q1", first.getZoneCode());
        assertEquals("https://static.flashscore.com/res/image/data/logo-arsenal.png", first.getLogoUrl());

        StandingRowSnapshot mid = snapshot.getRows().get(1);
        assertEquals("Brentford", mid.getExternalTeamName());
        assertNull(mid.getZoneCode());
        assertEquals(53, mid.getPoints());

        StandingRowSnapshot last = snapshot.getRows().get(2);
        assertEquals(18, last.getRank());
        assertEquals("West Ham", last.getExternalTeamName());
        assertEquals("r1", last.getZoneCode());
        assertEquals(39, last.getPoints());

        assertTrue(snapshot.getZoneRules().stream().anyMatch(rule ->
                "q1".equals(rule.getCode())
                        && "#004682".equals(rule.getColor())
                        && "Проход дальше - Лига чемпионов (Этап лиги)".equals(rule.getLabel())));
        assertTrue(snapshot.getZoneRules().stream().anyMatch(rule ->
                "r1".equals(rule.getCode())
                        && "#BD0000".equals(rule.getColor())
                        && "Зона вылета".equals(rule.getLabel())));
        assertNotNull(snapshot.getSourceUrl());
    }

    @Test
    void parseLegend_keepsProviderSpecificPlayoffWording() {
        String feed = "TR÷16¬TU÷r1¬TUC÷BD0000¬TN÷Stuttgart¬TI÷nJQmYp1B¬TM÷0¬TW÷0¬TDR÷0¬TL÷0¬TG÷0:0¬TP÷0¬"
                + "~TR÷17¬TU÷r3¬TUC÷FF4141¬TN÷Elversberg¬TI÷6in2Eknb¬TM÷0¬TW÷0¬TDR÷0¬TL÷0¬TG÷0:0¬TP÷0¬"
                + "~TV÷r1|Бундеслига (Понижение (плей-офф))|BD0000¬"
                + "TV÷r3|Зона вылета - Вторая Бундеслига|FF4141¬~";

        StandingsTableSnapshot snapshot = parser.parse(feed, "https://www.flashscore.com.ua/");

        assertEquals("r1", snapshot.getRows().get(0).getZoneCode());
        assertEquals("r3", snapshot.getRows().get(1).getZoneCode());
        assertTrue(snapshot.getZoneRules().stream().anyMatch(rule ->
                "r1".equals(rule.getCode())
                        && "Бундеслига (Понижение (плей-офф))".equals(rule.getLabel())
                        && "#BD0000".equals(rule.getColor())));
        assertTrue(snapshot.getZoneRules().stream().anyMatch(rule ->
                "r3".equals(rule.getCode())
                        && "Зона вылета - Вторая Бундеслига".equals(rule.getLabel())
                        && "#FF4141".equals(rule.getColor())));
    }
}
