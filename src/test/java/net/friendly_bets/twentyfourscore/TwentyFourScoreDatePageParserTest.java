package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwentyFourScoreDatePageParserTest {

    private final TwentyFourScoreDatePageParser parser = new TwentyFourScoreDatePageParser();

    @Test
    @DisplayName("parses competition header and finished score with HT")
    void parsesDateFixture() throws Exception {
        Path fixture = Path.of("src/test/resources/twentyfourscore/date-2026-06-15.html");
        String html = Files.readString(fixture, StandardCharsets.UTF_8);

        TwentyFourScoreParsedDatePage page = parser.parse(html);

        assertFalse(page.getCompetitions().isEmpty());
        assertTrue(page.getCompetitions().get(0).getTitle().toLowerCase().contains("мира")
                || page.getCompetitions().get(0).getTitle().toLowerCase().contains("world"));
        assertFalse(page.getCompetitions().get(0).getMatches().isEmpty());
        TwentyFourScoreParsedDatePage.MatchRow row = page.getCompetitions().get(0).getMatches().get(0);
        assertEquals("Швеция", row.getHomeName());
        assertEquals("Тунис", row.getAwayName());
        assertEquals("5:1", row.getFullTimeScore());
        assertEquals("2:1", row.getFirstTimeScore());
        assertEquals("FINISHED", row.getStatus());
        assertEquals("849893", row.getExternalMatchId());
    }
}
