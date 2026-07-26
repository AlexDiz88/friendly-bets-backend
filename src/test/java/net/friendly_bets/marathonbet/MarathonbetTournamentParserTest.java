package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetTournamentParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsePrematchEvents_skipsOutrightSeasonNodes() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode events = root.putArray("prematchEvents");

        ObjectNode match = events.addObject();
        match.put("treeId", 29306347L);
        match.put("eventId", 27595064L);
        match.put("name", "Арсенал - Ковентри Сити");
        match.putObject("homeTeam").putArray("members").addObject().put("name", "Арсенал");
        match.putObject("awayTeam").putArray("members").addObject().put("name", "Ковентри Сити");
        match.put("displayTime", 1787338800000L);

        ObjectNode outright = events.addObject();
        outright.put("treeId", 29311553L);
        outright.put("eventId", 27065384L);
        outright.put("name", "Премьер-лига. 2026/27");
        outright.putNull("homeTeam");
        outright.putNull("awayTeam");
        outright.put("displayTime", 1787410800000L);

        List<MarathonbetPrematchEvent> parsed = MarathonbetTournamentParser.parsePrematchEvents(root);

        assertEquals(1, parsed.size());
        assertEquals(29306347L, parsed.get(0).getTreeId());
        assertTrue(parsed.get(0).isMatchEvent());
    }
}
