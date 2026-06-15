package net.friendly_bets.wc26;

import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.friendly_bets.wc26.Wc26TeamCatalog;

class WcBerlinSlotMatchFilterTest {

    @Test
    void teamPairBelongsToSlot_r3s1_includesCzechMexico() {
        Team czech = Team.builder().id("cze").title("CzechRepublic").country("CZE").build();
        Team mexico = Team.builder().id("mex").title("Mexico").country("MEX").build();

        assertTrue(WcBerlinSlotMatchFilter.teamPairBelongsToSlot("3 [1]", czech, mexico));
        assertFalse(WcBerlinSlotMatchFilter.teamPairBelongsToSlot("3 [2]", czech, mexico));
    }

    @Test
    void filterGameResultRecords_r3s1_includesRecordWithOnlyInternalTeamIds() {
        GameResultRecord record = GameResultRecord.builder()
                .homeTeamId("cze-id")
                .awayTeamId("mex-id")
                .sources(Map.of())
                .build();
        Team czech = Team.builder().id("cze-id").title("CzechRepublic").country("CZE").build();
        Team mexico = Team.builder().id("mex-id").title("Mexico").country("MEX").build();

        List<GameResultRecord> filtered = WcBerlinSlotMatchFilter.filterGameResultRecords(
                "3 [1]",
                List.of(record),
                teamId -> {
                    if ("cze-id".equals(teamId)) {
                        return Optional.of(czech);
                    }
                    if ("mex-id".equals(teamId)) {
                        return Optional.of(mexico);
                    }
                    return Optional.empty();
                }
        );

        assertEquals(1, filtered.size());
    }

    @Test
    void filterGameResultRecords_r3s1_includesCzechMexicoWithRussianExternalNames() {
        GameResultRecord record = gameResultRecord(
                "Чехия",
                "cze-id",
                "Мексика",
                "mex-id"
        );
        Team czech = Team.builder().id("cze-id").title("CzechRepublic").country("CZE").build();
        Team mexico = Team.builder().id("mex-id").title("Mexico").country("MEX").build();

        List<GameResultRecord> filtered = WcBerlinSlotMatchFilter.filterGameResultRecords(
                "3 [1]",
                List.of(record),
                teamId -> {
                    if ("cze-id".equals(teamId)) {
                        return Optional.of(czech);
                    }
                    if ("mex-id".equals(teamId)) {
                        return Optional.of(mexico);
                    }
                    return Optional.empty();
                }
        );

        assertEquals(1, filtered.size());

        List<GameResultRecord> filteredByNamesOnly = WcBerlinSlotMatchFilter.filterGameResultRecords(
                "3 [1]",
                List.of(record)
        );
        assertEquals(1, filteredByNamesOnly.size());
    }

    @Test
    void teamPairBelongsToSlot_r1s2_includesBrazilMoroccoNotMexicoSouthAfrica() {
        Team brazil = Team.builder().id("bra").title("Brazil").country("BRA").build();
        Team morocco = Team.builder().id("mar").title("Morocco").country("MAR").build();
        Team mexico = Team.builder().id("mex").title("Mexico").country("MEX").build();
        Team rsa = Team.builder().id("rsa").title("SouthAfrica").country("RSA").build();

        assertTrue(WcBerlinSlotMatchFilter.teamPairBelongsToSlot("1 [2]", brazil, morocco));
        assertFalse(WcBerlinSlotMatchFilter.teamPairBelongsToSlot("1 [2]", mexico, rsa));
        assertFalse(WcBerlinSlotMatchFilter.teamPairBelongsToSlot("1 [1]", brazil, morocco));
    }

    @Test
    void expectedMatchCount_r3s1_isSix() {
        assertEquals(6, WcBerlinSlotMatchFilter.expectedMatchCount("3 [1]"));
        assertEquals(4, WcBerlinSlotMatchFilter.expectedMatchCount("2 [6]"));
    }

    @Test
    void scheduleIdsForSlot_r3s4_coversLastSixGroupMatches() {
        List<Integer> ids = WcTournamentSlots.scheduleIdsForSlot("3 [4]");
        assertEquals(6, ids.size());
        assertEquals(67, ids.get(0));
        assertEquals(72, ids.get(5));
    }

    @Test
    void fifaCodeForKnownName_recognizesInternalTitles() {
        assertTrue(Wc26TeamCatalog.fifaCodeForKnownName("SouthAfrica").isPresent());
        assertEquals("RSA", Wc26TeamCatalog.fifaCodeForKnownName("SouthAfrica").orElseThrow());
    }

    @Test
    void filterGameResultRecords_r1s4_includesSpainCapeVerdeIslands() {
        GameResultRecord record = gameResultRecord(
                "Spain",
                "esp-id",
                "Cape Verde Islands",
                "cpv-id"
        );

        List<GameResultRecord> filtered = WcBerlinSlotMatchFilter.filterGameResultRecords("1 [4]", List.of(record));

        assertEquals(1, filtered.size());
    }

    @Test
    void filterGameResultRecords_r1s6_includesPortugalCongoDrViaMappedTeamAliases() {
        GameResultRecord record = gameResultRecord(
                "Portugal",
                "por-id",
                "Congo DR",
                "cod-id"
        );
        Team congoDr = Team.builder()
                .id("cod-id")
                .title("Congo")
                .country("COD")
                .displayNames(TeamDisplayNames.builder()
                        .en("DR Congo")
                        .ru("ДР Конго")
                        .de("Kongo DR")
                        .build())
                .externalAliases(List.of(TeamExternalAlias.builder()
                        .provider("4score.ru")
                        .externalId(1934)
                        .externalName("Congo DR")
                        .build()))
                .build();

        List<GameResultRecord> filtered = WcBerlinSlotMatchFilter.filterGameResultRecords(
                "1 [6]",
                List.of(record),
                teamId -> {
                    if ("cod-id".equals(teamId)) {
                        return Optional.of(congoDr);
                    }
                    return Optional.empty();
                }
        );

        assertEquals(1, filtered.size());
    }

    @Test
    void filterGameResultRecords_r1s4_usesMappedTeamCountryWhenExternalNameUnknown() {
        GameResultRecord record = gameResultRecord(
                "Spain",
                "esp-id",
                "Some Unknown Label",
                "cpv-id"
        );
        Team capeVerde = Team.builder().id("cpv-id").title("CapeVerde").country("CPV").build();

        List<GameResultRecord> filtered = WcBerlinSlotMatchFilter.filterGameResultRecords(
                "1 [4]",
                List.of(record),
                teamId -> "cpv-id".equals(teamId) ? Optional.of(capeVerde) : Optional.empty()
        );

        assertEquals(1, filtered.size());
    }

    private static GameResultRecord gameResultRecord(
            String homeExternalName,
            String homeTeamId,
            String awayExternalName,
            String awayTeamId
    ) {
        return GameResultRecord.builder()
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        GameResultSourceSnapshot.builder()
                                .home(GameResultSideSnapshot.builder().externalName(homeExternalName).build())
                                .away(GameResultSideSnapshot.builder().externalName(awayExternalName).build())
                                .build()))
                .build();
    }
}
