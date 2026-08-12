package net.friendly_bets.flashscore;

import net.friendly_bets.flashscore.config.FlashscoreProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreTeamNamesServiceTest {

    @Test
    void extractTeamNamesFromDayFeedUsesCanonicalFhFkLabels() throws IOException {
        String feed = readFixture("flashscore/day-nurnberg-mini.feed");
        FlashscoreDayFeedParser parser = new FlashscoreDayFeedParser();
        FlashscoreParsedDayPage page = parser.parse(feed, LocalDate.of(2026, 8, 12));

        FlashscoreProperties.LeagueConfig bl = new FlashscoreProperties.LeagueConfig();
        bl.setStageId("6khmdCet");
        bl.setTitleContains("Bundesliga");

        var names = FlashscoreTeamNamesService.extractTeamNames(page, bl);

        assertTrue(names.contains("Nurnberg"));
        assertTrue(names.contains("SG Dynamo Dresden"));
        assertFalse(names.stream().anyMatch(name -> name.contains("Nurnberg") && name.contains("CX")));
    }

    @Test
    void competitionMatchesLeagueByStageIdOrTitle() {
        FlashscoreParsedDayPage.CompetitionBlock block = FlashscoreParsedDayPage.CompetitionBlock.builder()
                .title("GERMANY: 2. Bundesliga")
                .stageId("6khmdCet")
                .build();
        FlashscoreProperties.LeagueConfig bl = new FlashscoreProperties.LeagueConfig();
        bl.setStageId("6khmdCet");
        bl.setTitleContains("Bundesliga");

        assertTrue(FlashscoreTeamNamesService.competitionMatchesLeague(block, bl));
    }

    private static String readFixture(String resourcePath) throws IOException {
        Path path = Path.of("src/test/resources", resourcePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
