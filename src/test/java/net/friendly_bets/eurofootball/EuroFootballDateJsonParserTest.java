package net.friendly_bets.eurofootball;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.League;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EuroFootballDateJsonParserTest {

    private final EuroFootballDateJsonParser parser = new EuroFootballDateJsonParser(new ObjectMapper());

    @Test
    void mapStatus_usesStatusCodeAndPlainText() {
        assertEquals("IN_PLAY", EuroFootballDateJsonParser.mapStatus("live", 1, "44 мин"));
        assertEquals("IN_PLAY", EuroFootballDateJsonParser.mapStatus("live", 3, "88 мин"));
        assertEquals("PAUSED", EuroFootballDateJsonParser.mapStatus("live", 2, "Перерыв"));
        assertEquals("PAUSED", EuroFootballDateJsonParser.mapStatus("live", 3, "Перерыв"));
        assertEquals("EXTRA_TIME", EuroFootballDateJsonParser.mapStatus("live", 3, "доп. время 105 мин"));
        assertEquals("PENALTY_SHOOTOUT", EuroFootballDateJsonParser.mapStatus("live", 3, "Серия пенальти"));
        assertEquals("FINISHED", EuroFootballDateJsonParser.mapStatus("finished", 9, "Окончен"));
        assertEquals("FINISHED", EuroFootballDateJsonParser.mapStatus("finished", 16, "Окончен после пенальти"));
        assertEquals("SCHEDULED", EuroFootballDateJsonParser.mapStatus("soon", 0, "22:00"));
        assertEquals("CANCELED", EuroFootballDateJsonParser.mapStatus("live", 3, "Матч отменён"));
    }

    @Test
    void extractMinute_fromPlainStatusText() {
        assertEquals("88", EuroFootballDateJsonParser.extractMinuteLabel("IN_PLAY", "88 мин"));
        assertEquals("88", EuroFootballDateJsonParser.extractMinuteLabel("IN_PLAY", "2-й тайм 88 мин"));
        assertEquals("90+2", EuroFootballDateJsonParser.extractMinuteLabel("IN_PLAY", "90+2 мин"));
        assertEquals("105", EuroFootballDateJsonParser.extractMinuteLabel("EXTRA_TIME", "доп. время 105 мин"));
        assertNull(EuroFootballDateJsonParser.extractMinuteLabel("PAUSED", "Перерыв"));
        assertNull(EuroFootballDateJsonParser.extractMinuteLabel("FINISHED", "Окончен"));
    }

    @Test
    void plainStatusText_stripsHtmlMinuteSpan() {
        String html = "<span class=\"match-status-checkmark green\"></span>"
                + "<span class=\"match-status-text\">58<span class=\"minutes\"> мин</span></span>";
        assertEquals("58 мин", EuroFootballDateJsonParser.plainStatusText(html));
        assertEquals("Окончен", EuroFootballDateJsonParser.plainStatusText("Окончен"));
    }

    @Test
    void parse_fixtureKeepsEplSeparateFromOtherPremerLiga() throws Exception {
        String json;
        try (var in = Objects.requireNonNull(
                getClass().getResourceAsStream("/eurofootball/live-feed-sample.json"))) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        EuroFootballParsedDatePage page = parser.parse(json);
        assertEquals(3, page.getCompetitions().size());

        EuroFootballParsedDatePage.CompetitionBlock epl = page.getCompetitions().stream()
                .filter(b -> EuroFootballLeagueSupport.matchesTournament(
                        League.LeagueCode.EPL, b.getSlug(), b.getParentSlug()))
                .findFirst()
                .orElseThrow();
        assertEquals("premer-liga", epl.getSlug());
        assertEquals("angliya", epl.getParentSlug());
        assertEquals(1, epl.getMatches().size());
        EuroFootballParsedDatePage.MatchRow row = epl.getMatches().get(0);
        assertEquals("Кристал Пэлас", row.getHomeName());
        assertEquals("Манчестер Сити", row.getAwayName());
        assertEquals("IN_PLAY", row.getSnapshot().status());
        assertEquals("0:2", row.getSnapshot().fullTimeScore());

        long otherPremer = page.getCompetitions().stream()
                .filter(b -> "premer-liga".equals(b.getSlug()))
                .filter(b -> !EuroFootballLeagueSupport.matchesTournament(
                        League.LeagueCode.EPL, b.getSlug(), b.getParentSlug()))
                .count();
        assertEquals(1, otherPremer);

        List<EuroFootballParsedDatePage.MatchRow> eplRows = page.getCompetitions().stream()
                .filter(b -> EuroFootballLeagueSupport.matchesTournament(
                        League.LeagueCode.EPL, b.getSlug(), b.getParentSlug()))
                .flatMap(b -> b.getMatches().stream())
                .toList();
        assertFalse(eplRows.stream().anyMatch(m -> "Ли Ман Уорриорс".equals(m.getHomeName())));
        assertTrue(EuroFootballLeagueSupport.matchesTournament(
                League.LeagueCode.BL, "bundesliga", "germaniya"));
        assertFalse(EuroFootballLeagueSupport.matchesTournament(
                League.LeagueCode.BL, "bundesliga", "avstriya"));
    }
}
