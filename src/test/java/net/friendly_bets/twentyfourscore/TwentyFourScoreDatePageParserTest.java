package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    @DisplayName("home/away not swapped when first td is time (nth-of-type trap)")
    void parsesHomeAwayWithTimeColumn() {
        String html = """
                <table class="daymatches fbl">
                <tbody>
                	<tr><th colspan="5" class="champheader"><div class="champheader_title"><a>Россия Премьер-лига</a></div></th></tr>
                	<tr class="odd" id="row_853885">
                		<td class="time">14:00</td>
                		<td class="team"><a href="/football/team/russia/dynamo_moscow/"><span class="tm1">Динамо М</span></a></td>
                		<td class="team"><a href="/football/team/russia/krylya_sovetov/"><span class="tm2">Крылья Советов</span></a></td>
                		<td class="score"><a href="/football/match/853885-dynamo_moscow-krylya_sovetov" class="match"><b>0:0</b> (0:0)</a></td>
                	</tr>
                </tbody>
                </table>
                """;

        TwentyFourScoreParsedDatePage page = parser.parse(html);
        assertFalse(page.getCompetitions().isEmpty());
        assertFalse(page.getCompetitions().get(0).getMatches().isEmpty());
        TwentyFourScoreParsedDatePage.MatchRow row = page.getCompetitions().get(0).getMatches().get(0);
        assertEquals("Динамо М", row.getHomeName());
        assertEquals("Крылья Советов", row.getAwayName());
        assertEquals("853885", row.getExternalMatchId());
    }

    @Test
    @DisplayName("live minute stored without apostrophe; halftime → PAUSED")
    void parsesLiveMinuteAndHalftime() {
        String liveHtml = """
                <table class="daymatches fbl"><tbody>
                <tr><th class="champheader"><div class="champheader_title"><a>Test</a></div></th></tr>
                <tr>
                 <td class="team"><span class="tm1">A</span></td>
                 <td class="team"><span class="tm2">B</span></td>
                 <td class="score"><b>1:0</b> 72'</td>
                </tr>
                </tbody></table>
                """;
        TwentyFourScoreParsedDatePage.MatchRow live = parser.parse(liveHtml).getCompetitions().get(0).getMatches().get(0);
        assertEquals("72", live.getLiveMinuteLabel());
        assertEquals("LIVE", live.getStatus());

        String htHtml = """
                <table class="daymatches fbl"><tbody>
                <tr><th class="champheader"><div class="champheader_title"><a>Test</a></div></th></tr>
                <tr>
                 <td class="team"><span class="tm1">A</span></td>
                 <td class="team"><span class="tm2">B</span></td>
                 <td class="score"><b>1:0</b> (1:0) Перерыв</td>
                </tr>
                </tbody></table>
                """;
        TwentyFourScoreParsedDatePage.MatchRow ht = parser.parse(htHtml).getCompetitions().get(0).getMatches().get(0);
        assertNull(ht.getLiveMinuteLabel());
        assertEquals("PAUSED", ht.getStatus());

        String ht24scoreHtml = """
                <table class="daymatches fbl"><tbody>
                <tr><th class="champheader"><div class="champheader_title"><a>Test</a></div></th></tr>
                <tr>
                 <td class="team"><span class="tm1">ЙИППО</span></td>
                 <td class="team"><span class="tm2">ЯПС</span></td>
                 <td class="score"><b>0:0</b> (0:0) HT</td>
                </tr>
                </tbody></table>
                """;
        TwentyFourScoreParsedDatePage.MatchRow htEn = parser.parse(ht24scoreHtml).getCompetitions().get(0).getMatches().get(0);
        assertNull(htEn.getLiveMinuteLabel());
        assertEquals("PAUSED", htEn.getStatus());
        assertEquals("0:0", htEn.getFullTimeScore());
    }
}
