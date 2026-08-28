package net.friendly_bets.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSiteAccessProbeServiceTest {

    @Test
    void looksLikeGraphqlEmptyQuery_detectsEngineRejection() {
        assertTrue(ExternalSiteAccessProbeService.looksLikeGraphqlEmptyQuery(
                400, "{\"error\": \"no queries to execute\"}"));
        assertFalse(ExternalSiteAccessProbeService.looksLikeGraphqlEmptyQuery(
                400, "<html>Just a moment</html>"));
        assertFalse(ExternalSiteAccessProbeService.looksLikeGraphqlEmptyQuery(
                403, "{\"error\":\"no queries to execute\"}"));
    }

    @Test
    void snippet_stripsTagsAndKeepsMatchText() {
        String body = """
                <html><head><style>.x{color:red}</style><script>var a=1</script></head>
                <body>
                  <nav>Меню сайта</nav>
                  <div class="MatchRow-module_match-row__Stat">
                    <span class="MatchRow-module_match-row__team-name">Бавария</span>
                    <div class="MatchRow-module_match-row__score"><span>1</span>:<span>0</span></div>
                    <span class="MatchRow-module_match-row__team-name">Штутгарт</span>
                    <div class="MatchRow-module_match-row__period">Перерыв</div>
                  </div>
                </body></html>
                """;
        String snippet = ExternalSiteAccessProbeService.snippet(body);
        assertFalse(snippet.contains("<div"));
        assertFalse(snippet.contains("var a=1"));
        assertTrue(snippet.contains("Бавария"));
        assertTrue(snippet.contains("Штутгарт"));
        assertTrue(snippet.contains("1 : 0") || snippet.contains("1:0"));
        assertTrue(snippet.contains("Перерыв"));
    }

    @Test
    void snippet_keepsJsonPayload() {
        String json = "{\"error\": \"no queries to execute\"}";
        assertEquals(json, ExternalSiteAccessProbeService.snippet(json));
    }
}
