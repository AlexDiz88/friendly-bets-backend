package net.friendly_bets.aiscore;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiscoreScheduleParserTest {

    private final AiscoreScheduleParser parser = new AiscoreScheduleParser();

    @Test
    void parsesDcMatchesWithTeamCatalogById() {
        // Real aiscore shape: (function(params){ body }(args)); matches in dC[n]=;
        // most rows have homeTeam:{id:X} only; names come from {id,name,slug} catalog.
        String html = """
                <html><body><script>
                window.__NUXT__=(function(a,b,c,d,e,f,g,h,i,j,k){
                dC[0]={id:"m1",sportId:a,homeTeam:{id:b},awayTeam:{id:c},matchTime:d,statusId:a,roundNum:e};
                dC[1]={id:"m2",sportId:a,homeTeam:{id:c},awayTeam:{id:f},matchTime:d,statusId:a,roundNum:g};
                teamMap={t0:{id:b,name:h,slug:i},t1:{id:c,name:j,slug:i},t2:{id:f,name:k,slug:i}};
                }(1,"h1","a1",1755806400,1,"a2",2,"Arsenal","arsenal","Chelsea","Liverpool"));
                </script></body></html>
                """;

        AiscoreParsedSchedule parsed = parser.parse(html, "tournament-english-premier-league/mo07dni2vfxknxy");
        assertEquals(2, parsed.getRounds().size());
        assertEquals(1, parsed.getRounds().get(0).getNumber());
        assertEquals(1, parsed.getRounds().get(0).getMatches().size());

        AiscoreParsedSchedule.Match match = parsed.getRounds().get(0).getMatches().get(0);
        assertEquals("Arsenal", match.getHomeName());
        assertEquals("Chelsea", match.getAwayName());
        assertEquals(Instant.ofEpochSecond(1755806400L), match.getUtcKickoff());
        assertEquals("m1", match.getAiscoreMatchId());
        assertEquals("SCHEDULED", match.getStatus());

        AiscoreParsedSchedule.Match round2 = parsed.getRounds().get(1).getMatches().get(0);
        assertEquals(2, parsed.getRounds().get(1).getNumber());
        assertEquals("Chelsea", round2.getHomeName());
        assertEquals("Liverpool", round2.getAwayName());
    }

    @Test
    void parseAllTeamNames_returnsUniqueNames() {
        String html = """
                <html><body><script>
                window.__NUXT__=(function(a,b,c,d,e,f,g,h,i,j,k){
                dC[0]={id:"m1",homeTeam:{id:b},awayTeam:{id:c},matchTime:d,statusId:a,roundNum:e};
                dC[1]={id:"m2",homeTeam:{id:c},awayTeam:{id:f},matchTime:d,statusId:a,roundNum:g};
                x={id:b,name:h,slug:i};y={id:c,name:j,slug:i};z={id:f,name:k,slug:i};
                }(1,"h1","a1",1755806400,1,"a2",2,"Arsenal","arsenal","Chelsea","Liverpool"));
                </script></body></html>
                """;

        List<String> names = parser.parseAllTeamNames(html, "path");
        assertEquals(3, names.size());
        assertTrue(names.contains("Arsenal"));
        assertTrue(names.contains("Chelsea"));
        assertTrue(names.contains("Liverpool"));
    }

    @Test
    void invalidNuxt_throws() {
        String html = "<script>window.__NUXT__={broken:true};</script>";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(html, "path"));
    }
}
