package net.friendly_bets.fourscore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourScoreMatchListParserTest {

    private final FourScoreMatchListParser parser = new FourScoreMatchListParser();

    @Test
    void parsesWorldCupAndFriendliesFromFixture() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/fourscore/events-2026-06-11.html"));
        List<FourScoreListMatch> matches = parser.parse(
                html,
                EnumSet.of(FourScoreLeagueSection.WORLD_CUP, FourScoreLeagueSection.FRIENDLIES)
        );

        assertTrue(matches.stream().anyMatch(m ->
                "Мексика".equals(m.getHomeTeamName())
                        && "Южная Африка".equals(m.getAwayTeamName())
                        && m.getSection() == FourScoreLeagueSection.WORLD_CUP));
        assertTrue(matches.stream().anyMatch(m ->
                "Англия".equals(m.getHomeTeamName())
                        && "Коста-Рика".equals(m.getAwayTeamName())
                        && "angliya-costa-rica-10-06-2026".equals(m.getEventSlug())
                        && m.isTerminal()));
    }

    @Test
    void slugFromPath_normalizesEventsPrefix() {
        assertEquals("angliya-costa-rica-10-06-2026",
                FourScoreMatchListParser.slugFromPath("/events/angliya-costa-rica-10-06-2026/"));
    }
}
