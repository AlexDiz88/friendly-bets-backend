package net.friendly_bets.eurofootball;

import net.friendly_bets.models.League;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EuroFootballDateHtmlParserTest {

    private final EuroFootballDateHtmlParser parser = new EuroFootballDateHtmlParser();

    @Test
    void parse_mainScheduleFeed_ignoresSidebarClass() {
        String html = """
                <div class="match-schedule__item">
                  <div class="match-schedule__item-header">
                    <a class="match-schedule__item-header__title" href="/online/angliya">Англия</a>
                    <a class="match-schedule__item-header__title" href="/online/angliya/premer-liga">Премьер-Лига</a>
                  </div>
                  <div class="match-schedule__item-container">
                    <div class="match-online-list__item" data-status="finished" data-match-id="1">
                      <a class="match-online-list__item-status">Окончен</a>
                      <div class="match-online-list__item-name">
                        <div class="team1name"><span>Астон Вилла</span></div>
                        <div class="team2name"><span>Арсенал</span></div>
                        <div class="team__score">
                          <a class="item-score-link"><div class="goal-team-block">0</div><div class="goal-team-block">1</div></a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="liveresult">
                  <div class="match-online__item">
                    <div class="team1name"><span>Астон Вилла</span></div>
                    <div class="team2name"><span>Арсенал</span></div>
                  </div>
                </div>
                """;
        EuroFootballParsedDatePage page = parser.parse(html);
        assertEquals(1, page.getCompetitions().size());
        EuroFootballParsedDatePage.CompetitionBlock epl = page.getCompetitions().get(0);
        assertEquals("premer-liga", epl.getSlug());
        assertEquals("angliya", epl.getParentSlug());
        assertEquals(1, epl.getMatches().size());
        assertEquals("Астон Вилла", epl.getMatches().get(0).getHomeName());
        assertEquals("FINISHED", epl.getMatches().get(0).getSnapshot().status());
        assertEquals("0:1", epl.getMatches().get(0).getSnapshot().fullTimeScore());
        assertTrue(EuroFootballLeagueSupport.matchesTournament(
                League.LeagueCode.EPL, epl.getSlug(), epl.getParentSlug()));
    }

    @Test
    void slugFromHref_parsesParentAndLeague() {
        assertEquals("angliya", EuroFootballDateHtmlParser.slugFromHref("/online/angliya", true));
        assertEquals("premer-liga", EuroFootballDateHtmlParser.slugFromHref("/online/angliya/premer-liga", false));
        assertEquals("liga_chempionov", EuroFootballDateHtmlParser.slugFromHref("/online/liga_chempionov", false));
    }
}
