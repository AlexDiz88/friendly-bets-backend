package net.friendly_bets.wc26;

import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Keeps only matches belonging to a Berlin betting slot ({@code 1 [1]} … {@code 3 [4]}).
 */
public final class WcBerlinSlotMatchFilter {

    private WcBerlinSlotMatchFilter() {
    }

    public static boolean isBerlinGroupSlot(String slotId) {
        return WcTournamentSlots.isBerlinGroupSlot(slotId);
    }

    public static int expectedMatchCount(String slotId) {
        if (!isBerlinGroupSlot(slotId)) {
            return 0;
        }
        return WcTournamentSlots.scheduleIdsForSlot(slotId).size();
    }

    public static List<FootballDataMatchDto> filterFootballDataMatches(
            String slotId,
            List<FootballDataMatchDto> matches
    ) {
        if (!isBerlinGroupSlot(slotId) || matches == null || matches.isEmpty()) {
            return matches == null ? List.of() : matches;
        }
        Set<ExpectedPair> expected = expectedPairsForSlot(slotId);
        List<FootballDataMatchDto> filtered = new ArrayList<>();
        for (FootballDataMatchDto match : matches) {
            if (match == null || match.getHomeTeam() == null || match.getAwayTeam() == null) {
                continue;
            }
            FootballDataMatchDto.Team home = match.getHomeTeam();
            FootballDataMatchDto.Team away = match.getAwayTeam();
            if (matchesExpectedPair(
                    expected,
                    home.getTla(),
                    home.getName(),
                    away.getTla(),
                    away.getName()
            )) {
                filtered.add(match);
            }
        }
        return filtered;
    }

    public static List<GameResultRecord> filterGameResultRecords(
            String slotId,
            List<GameResultRecord> records
    ) {
        return filterGameResultRecords(slotId, records, null);
    }

    public static List<GameResultRecord> filterGameResultRecords(
            String slotId,
            List<GameResultRecord> records,
            Function<String, Optional<Team>> teamById
    ) {
        if (!isBerlinGroupSlot(slotId) || records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }
        Set<ExpectedPair> expected = expectedPairsForSlot(slotId);
        List<GameResultRecord> filtered = new ArrayList<>();
        for (GameResultRecord record : records) {
            GameResultSourceSnapshot source = record.footballDataSource();
            if (source == null) {
                continue;
            }
            if (matchesExpectedPair(expected, record, source, teamById)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private static Set<ExpectedPair> expectedPairsForSlot(String slotId) {
        Set<ExpectedPair> pairs = new HashSet<>();
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            Wc26ScheduleCatalog.findById(scheduleId).ifPresent(match ->
                    pairs.add(new ExpectedPair(match.homeFifa(), match.awayFifa()))
            );
        }
        return pairs;
    }

    private static boolean matchesExpectedPair(
            Set<ExpectedPair> expected,
            String homeTla,
            String homeName,
            String awayTla,
            String awayName
    ) {
        for (ExpectedPair pair : expected) {
            if (fifaMatches(homeTla, homeName, pair.homeFifa())
                    && fifaMatches(awayTla, awayName, pair.awayFifa())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExpectedPair(
            Set<ExpectedPair> expected,
            GameResultRecord record,
            GameResultSourceSnapshot source,
            Function<String, Optional<Team>> teamById
    ) {
        String homeName = sideExternalName(source.getHome());
        String awayName = sideExternalName(source.getAway());
        for (ExpectedPair pair : expected) {
            if (sideMatchesFifa(null, homeName, record.getHomeTeamId(), teamById, pair.homeFifa())
                    && sideMatchesFifa(null, awayName, record.getAwayTeamId(), teamById, pair.awayFifa())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sideMatchesFifa(
            String tla,
            String externalName,
            String teamId,
            Function<String, Optional<Team>> teamById,
            String fifaCode
    ) {
        if (fifaMatches(tla, externalName, fifaCode)) {
            return true;
        }
        if (teamById == null || teamId == null || teamId.isBlank()) {
            return false;
        }
        return teamById.apply(teamId)
                .map(team -> teamMatchesFifa(team, fifaCode))
                .orElse(false);
    }

    private static boolean teamMatchesFifa(Team team, String fifaCode) {
        if (team.getCountry() != null && team.getCountry().equalsIgnoreCase(fifaCode)) {
            return true;
        }
        if (team.getTitle() != null && fifaMatches(null, team.getTitle(), fifaCode)) {
            return true;
        }
        if (displayNamesMatchFifa(team.getDisplayNames(), fifaCode)) {
            return true;
        }
        if (externalAliasesMatchFifa(team.getExternalAliases(), fifaCode)) {
            return true;
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle())
                .map(code -> code.equalsIgnoreCase(fifaCode))
                .orElse(false);
    }

    private static boolean displayNamesMatchFifa(TeamDisplayNames displayNames, String fifaCode) {
        if (displayNames == null) {
            return false;
        }
        return fifaMatches(null, displayNames.getEn(), fifaCode)
                || fifaMatches(null, displayNames.getRu(), fifaCode)
                || fifaMatches(null, displayNames.getDe(), fifaCode);
    }

    private static boolean externalAliasesMatchFifa(List<TeamExternalAlias> aliases, String fifaCode) {
        if (aliases == null || aliases.isEmpty()) {
            return false;
        }
        for (TeamExternalAlias alias : aliases) {
            if (alias != null && fifaMatches(null, alias.getExternalName(), fifaCode)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fifaMatches(String tla, String name, String fifaCode) {
        return Wc26TeamCatalog.nameMatchesFifaCode(name, tla, fifaCode);
    }

    private static String sideExternalName(GameResultSideSnapshot side) {
        return side != null && side.getExternalName() != null ? side.getExternalName().trim() : null;
    }

    private record ExpectedPair(String homeFifa, String awayFifa) {
    }
}
