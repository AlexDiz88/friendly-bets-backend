package net.friendly_bets.fourscore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FourScoreEventPageParserTest {

    private final FourScoreEventPageParser parser = new FourScoreEventPageParser();
    private final FourScoreScoreNormalizer normalizer = new FourScoreScoreNormalizer();

    @Test
    void parsesRegularMatchHalves() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/fourscore/event-angliya-costa-rica.html"));
        FourScoreEventDetails details = parser.parse(
                html,
                "/events/angliya-costa-rica-10-06-2026/",
                FourScoreLeagueSection.FRIENDLIES
        );
        assertNotNull(details);
        assertEquals("Англия", details.getHomeTeamName());
        assertEquals("Коста-Рика", details.getAwayTeamName());
        assertEquals("1:0", details.getFirstHalfScore());
        assertEquals("2:0", details.getSecondHalfScore());

        FourScoreScoreNormalizer.NormalizedScore normalized = normalizer.normalize(details);
        assertNotNull(normalized);
        assertEquals("3:0", normalized.gameScore().getFullTime());
        assertEquals("1:0", normalized.gameScore().getFirstTime());
        assertEquals("FINISHED", normalized.status());
    }

    @Test
    void parsesSaudiSenegalDraw() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/fourscore/event-saudi-senegal.html"));
        FourScoreEventDetails details = parser.parse(
                html,
                "/events/saudi-arabia-senegal-09-06-2026/",
                FourScoreLeagueSection.FRIENDLIES
        );
        assertNotNull(details);
        assertEquals("Саудовская Аравия", details.getHomeTeamName());
        assertEquals("Сенегал", details.getAwayTeamName());
        assertEquals("0:0", details.getFirstHalfScore());
        assertEquals("0:0", details.getSecondHalfScore());
    }

    @Test
    void parsesPenaltyShootoutMatch() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/fourscore/event-wales-bosnia-penalties.html"));
        FourScoreEventDetails details = parser.parse(
                html,
                "/events/uels-bosniya-i-gertsegovina-26-03-2026/",
                FourScoreLeagueSection.FRIENDLIES
        );
        assertNotNull(details);
        assertEquals("0:0", details.getFirstHalfScore());
        assertEquals("1:1", details.getSecondHalfScore());
        assertEquals("0:0", details.getExtraTimeScore());
        assertEquals("2:4", details.getPenaltyScore());

        FourScoreScoreNormalizer.NormalizedScore normalized = normalizer.normalize(details);
        assertNotNull(normalized);
        assertEquals("1:1", normalized.gameScore().getFullTime());
        assertEquals("2:4", normalized.gameScore().getPenalty());
    }
}
