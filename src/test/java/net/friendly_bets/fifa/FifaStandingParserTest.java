package net.friendly_bets.fifa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FifaStandingParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesGroupLetterAndOfficialPosition() throws Exception {
        String json = """
                {
                  "Group": [{"Locale": "en-GB", "Description": "Group B"}],
                  "Position": 1,
                  "Played": 1,
                  "Won": 0,
                  "Drawn": 1,
                  "Lost": 0,
                  "For": 1,
                  "Against": 1,
                  "GoalsDiference": 0,
                  "Points": 1,
                  "Team": {"Abbreviation": "SUI", "IdTeam": "43971"}
                }
                """;
        var row = objectMapper.readTree(json);

        assertEquals("B", FifaStandingParser.groupLetter(row));
        assertEquals("SUI", FifaStandingParser.teamCode(row));
        assertEquals(1, FifaStandingParser.position(row));
        assertEquals(1, FifaStandingParser.points(row));
    }
}
