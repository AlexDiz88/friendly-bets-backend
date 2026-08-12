package net.friendly_bets.liveresult;

import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveresultStandingsParserTest {

  private final LiveresultStandingsParser parser = new LiveresultStandingsParser();

  @Test
  void parseEplStandings_extractsRowsAndZoneRules() throws IOException {
    String html = Files.readString(
        Path.of("src/test/resources/liveresult/epl-standings.html"),
        StandardCharsets.UTF_8
    );

    StandingsTableSnapshot snapshot = parser.parse(html, "https://www.liveresult.ru/football/England/Premier-League/standings");

    assertNotNull(snapshot);
    assertFalse(snapshot.getRows().isEmpty());
    assertTrue(snapshot.getRows().size() >= 18);
    assertFalse(snapshot.getZoneRules().isEmpty());

    StandingRowSnapshot first = snapshot.getRows().get(0);
    assertEquals(1, first.getRank());
    assertNotNull(first.getExternalTeamName());
    assertNotNull(first.getLogoUrl());
    assertEquals("bg-success", first.getZoneCode());

    assertTrue(snapshot.getZoneRules().stream().anyMatch(rule -> "bg-success".equals(rule.getCode())));
    assertTrue(snapshot.getZoneRules().stream().anyMatch(rule -> "bg-danger".equals(rule.getCode())));
  }
}
