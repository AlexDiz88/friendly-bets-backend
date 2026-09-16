package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwentyFourScoreEuropaLiveFixtureTest {

    private final TwentyFourScoreDatePageParser parser = new TwentyFourScoreDatePageParser();

    @Test
    @DisplayName("LE header with odds th columns: title stays Europa; HT rows under LE")
    void parsesEuropaLeagueLiveRowsBesideOddsHeader() throws Exception {
        Path fixture = Path.of("src/test/resources/twentyfourscore/date-2026-09-16-europa-live.html");
        String html = Files.readString(fixture, StandardCharsets.UTF_8);

        TwentyFourScoreParsedDatePage page = parser.parse(html);

        var leBlocks = page.getCompetitions().stream()
                .filter(b -> TwentyFourScoreLeagueTitles.matches(League.LeagueCode.LE, b.getTitle()))
                .toList();
        assertFalse(leBlocks.isEmpty(), "LE competition block missing; titles="
                + page.getCompetitions().stream().map(TwentyFourScoreParsedDatePage.CompetitionBlock::getTitle).toList());

        String leTitle = leBlocks.get(0).getTitle();
        assertTrue(leTitle.toLowerCase().contains("лига европы"), leTitle);
        assertFalse(leTitle.contains("2.8"), "title must not include stats totals: " + leTitle);

        var rows = leBlocks.stream().flatMap(b -> b.getMatches().stream()).toList();
        assertEquals(2, rows.size(), "expected LE matches, got " + rows.size()
                + " homes=" + rows.stream().map(r -> r.getHomeName() + "-" + r.getAwayName()).toList());

        var ararat = rows.stream()
                .filter(r -> "Арарат-Армения".equals(r.getHomeName()) && "Спарта Пр".equals(r.getAwayName()))
                .findFirst();
        assertTrue(ararat.isPresent());
        assertEquals("0:2", ararat.get().getFullTimeScore());
        assertEquals("PAUSED", ararat.get().getStatus());

        var omonia = rows.stream()
                .filter(r -> "Омония".equals(r.getHomeName()) && "Сельта".equals(r.getAwayName()))
                .findFirst();
        assertTrue(omonia.isPresent());
        assertEquals("0:0", omonia.get().getFullTimeScore());
        assertEquals("PAUSED", omonia.get().getStatus());
    }
}
