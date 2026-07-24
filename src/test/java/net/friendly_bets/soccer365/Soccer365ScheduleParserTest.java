package net.friendly_bets.soccer365;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Soccer365ScheduleParserTest {

    private final Soccer365ScheduleParser parser = new Soccer365ScheduleParser();

    @Test
    void parsesRoundMatchesAndClubFilterFromHtml() {
        String html = """
                <div class="selectbox-menu">
                  <a class="selectbox-selected" href="javascript:void(0)" onclick="filtersData('club',0);">Все команды</a>
                  <a href="javascript:void(0)" onclick="filtersData('club','2');">Арсенал</a>
                  <a href="javascript:void(0)" onclick="filtersData('club','6899');">Ковентри Сити</a>
                </div>
                <div class="cmp_stg_ttl">1-й тур</div>
                <div class="game_block " dt-ht="2" dt-at="6899">
                  <script type="application/ld+json">{"@type":"Event","startDate":"2026-08-21T22:00:00+03:00"}</script>
                  <div class="status"><span class="size10">21.08, 21:00</span></div>
                  <div class="result">
                    <div class="ht"><div class="name"><div class="img16"><span>Арсенал</span></div></div><div class="gls">-</div></div>
                    <div class="at"><div class="name"><div class="img16"><span>Ковентри Сити</span></div></div><div class="gls">-</div></div>
                  </div>
                </div>
                <div class="cmp_stg_ttl">31-й тур</div>
                <div class="game_block">
                  <div class="status"><span class="size10">01.01, 12:00</span></div>
                  <div class="result">
                    <div class="ht"><div class="name"><span>Челси</span></div><div class="gls">-</div></div>
                    <div class="at"><div class="name"><span>Ливерпуль</span></div><div class="gls">-</div></div>
                  </div>
                </div>
                """;

        Soccer365ParsedSchedule parsed = parser.parse(html, 12);
        assertEquals(2, parsed.getRounds().size());
        assertEquals(1, parsed.getRounds().get(0).getNumber());
        assertEquals(1, parsed.getRounds().get(0).getMatches().size());
        Soccer365ParsedSchedule.Match match = parsed.getRounds().get(0).getMatches().get(0);
        assertEquals("Арсенал", match.getHomeName());
        assertEquals("Ковентри Сити", match.getAwayName());
        assertEquals(Instant.parse("2026-08-21T19:00:00Z"), match.getUtcKickoff());
        assertEquals("SCHEDULED", match.getStatus());
        assertEquals(2, parsed.getClubFilterNames().size());
        assertTrue(parsed.getClubFilterNames().contains("Арсенал"));
    }
}
