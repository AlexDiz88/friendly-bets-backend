package net.friendly_bets.wc26;

import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.friendly_bets.wc26.Wc26TeamCatalog;

class WcBerlinSlotMatchFilterTest {

    @Test
    void filterFootballDataMatches_r1s1_returnsFourOpeningMatches() {
        List<FootballDataMatchDto> all = List.of(
                match("Mexico", "MEX", "South Africa", "RSA"),
                match("Korea Republic", "KOR", "Czechia", "CZE"),
                match("Canada", "CAN", "Bosnia and Herzegovina", "BIH"),
                match("USA", "USA", "Paraguay", "PAR"),
                match("Qatar", "QAT", "Switzerland", "SUI"),
                match("Brazil", "BRA", "Morocco", "MAR")
        );

        List<FootballDataMatchDto> filtered = WcBerlinSlotMatchFilter.filterFootballDataMatches("1 [1]", all);

        assertEquals(4, filtered.size());
        assertTrue(filtered.stream().anyMatch(m -> "Mexico".equals(m.getHomeTeam().getName())));
        assertTrue(filtered.stream().noneMatch(m -> "Qatar".equals(m.getHomeTeam().getName())));
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
    void filterFootballDataMatches_r1s1_internalTeamTitles() {
        List<FootballDataMatchDto> all = List.of(
                match("Mexico", "MEX", "SouthAfrica", "RSA"),
                match("KoreaRepublic", "KOR", "CzechRepublic", "CZE"),
                match("Canada", "CAN", "Bosnia", "BIH"),
                match("UnitedStates", "USA", "Paraguay", "PAR")
        );

        List<FootballDataMatchDto> filtered = WcBerlinSlotMatchFilter.filterFootballDataMatches("1 [1]", all);

        assertEquals(4, filtered.size());
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
                        .provider("football-data")
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
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                        GameResultSourceSnapshot.builder()
                                .home(GameResultSideSnapshot.builder().externalName(homeExternalName).build())
                                .away(GameResultSideSnapshot.builder().externalName(awayExternalName).build())
                                .build()))
                .build();
    }

    private static FootballDataMatchDto match(
            String homeName,
            String homeTla,
            String awayName,
            String awayTla
    ) {
        FootballDataMatchDto dto = new FootballDataMatchDto();
        FootballDataMatchDto.Team home = new FootballDataMatchDto.Team();
        home.setName(homeName);
        home.setTla(homeTla);
        FootballDataMatchDto.Team away = new FootballDataMatchDto.Team();
        away.setName(awayName);
        away.setTla(awayTla);
        dto.setHomeTeam(home);
        dto.setAwayTeam(away);
        return dto;
    }
}
