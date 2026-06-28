package net.friendly_bets.wc26;

import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;

import net.friendly_bets.fourscore.FourScorePlayoffPlaceholderNames;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Keeps only matches belonging to a WC betting slot ({@code 1 [1]} … {@code 3 [4]}, {@code 1/16 [1]}, …).
 */
public final class WcBerlinSlotMatchFilter {

    private static final long KICKOFF_MATCH_WINDOW_MINUTES = 180;

    private WcBerlinSlotMatchFilter() {
    }

    public static boolean isBerlinGroupSlot(String slotId) {
        return WcTournamentSlots.isBerlinGroupSlot(slotId);
    }

    public static boolean isWcBettingSlot(String slotId) {
        return WcTournamentSlots.isWcBettingSlot(slotId);
    }

    public static boolean isPlayoffSlot(String slotId) {
        return WcTournamentSlots.isPlayoffSlot(slotId);
    }

    public static int expectedMatchCount(String slotId) {
        if (!isWcBettingSlot(slotId)) {
            return 0;
        }
        return WcTournamentSlots.scheduleIdsForSlot(slotId).size();
    }

    /** Whether internal home/away teams are one of the pairs in a WC betting slot. */
    public static boolean teamPairBelongsToSlot(String slotId, Team home, Team away) {
        if (!isWcBettingSlot(slotId) || home == null || away == null) {
            return false;
        }
        Set<ExpectedPair> expected = expectedPairsForSlot(slotId);
        for (ExpectedPair pair : expected) {
            if (teamMatchesFifa(home, pair.homeFifa()) && teamMatchesFifa(away, pair.awayFifa())) {
                return true;
            }
        }
        return false;
    }

    public static boolean recordBelongsToSlot(
            String slotId,
            GameResultRecord record,
            Function<String, Optional<Team>> teamById
    ) {
        if (!isWcBettingSlot(slotId) || record == null) {
            return false;
        }
        Integer scheduleId = record.getWc26ScheduleId();
        if (isPlayoffSlot(slotId)) {
            return scheduleId != null && WcTournamentSlots.scheduleIdsForSlot(slotId).contains(scheduleId);
        }
        if (scheduleId != null && WcTournamentSlots.scheduleIdsForSlot(slotId).contains(scheduleId)) {
            return true;
        }
        if (scheduleId != null && WcTournamentSlots.belongsToAnotherPlayoffSlot(slotId, scheduleId)) {
            return false;
        }
        Set<ExpectedPair> expected = expectedPairsForSlot(slotId);
        if (matchesExpectedPair(expected, record, record.primaryExternalSource(), teamById)) {
            return true;
        }
        if (kickoffBelongsToSlot(slotId, record.getUtcDate())) {
            return true;
        }
        return false;
    }

    public static boolean matchBelongsToWcSlot(
            String slotId,
            Team home,
            Team away,
            LocalDateTime kickoff,
            Integer wc26ScheduleId
    ) {
        if (!isWcBettingSlot(slotId)) {
            return true;
        }
        if (isPlayoffSlot(slotId)) {
            return wc26ScheduleId != null && WcTournamentSlots.scheduleIdsForSlot(slotId).contains(wc26ScheduleId);
        }
        if (wc26ScheduleId != null && WcTournamentSlots.scheduleIdsForSlot(slotId).contains(wc26ScheduleId)) {
            return true;
        }
        if (wc26ScheduleId != null && WcTournamentSlots.belongsToAnotherPlayoffSlot(slotId, wc26ScheduleId)) {
            return false;
        }
        if (teamPairBelongsToSlot(slotId, home, away)) {
            return true;
        }
        if (kickoffBelongsToSlot(slotId, kickoff)) {
            return true;
        }
        return false;
    }

    public static Optional<Integer> resolveScheduleIdInSlot(String slotId, LocalDateTime kickoff) {
        if (!isWcBettingSlot(slotId) || kickoff == null) {
            return Optional.empty();
        }
        Optional<Integer> bestId = Optional.empty();
        long bestDeltaMinutes = Long.MAX_VALUE;
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            Optional<LocalDateTime> slotKickoff = Wc26ScheduleKickoffLookup.kickoffUtc(scheduleId);
            if (slotKickoff.isEmpty()) {
                continue;
            }
            long deltaMinutes = Math.abs(Duration.between(kickoff, slotKickoff.get()).toMinutes());
            if (deltaMinutes <= KICKOFF_MATCH_WINDOW_MINUTES && deltaMinutes < bestDeltaMinutes) {
                bestDeltaMinutes = deltaMinutes;
                bestId = Optional.of(scheduleId);
            }
        }
        return bestId;
    }

    public static Optional<Integer> resolvePlayoffScheduleIdByKickoff(LocalDateTime kickoff) {
        if (kickoff == null) {
            return Optional.empty();
        }
        Optional<Integer> bestId = Optional.empty();
        long bestDeltaMinutes = Long.MAX_VALUE;
        for (int scheduleId = 73; scheduleId <= 104; scheduleId++) {
            Optional<LocalDateTime> slotKickoff = Wc26ScheduleKickoffLookup.kickoffUtc(scheduleId);
            if (slotKickoff.isEmpty()) {
                continue;
            }
            long deltaMinutes = Math.abs(Duration.between(kickoff, slotKickoff.get()).toMinutes());
            if (deltaMinutes <= KICKOFF_MATCH_WINDOW_MINUTES && deltaMinutes < bestDeltaMinutes) {
                bestDeltaMinutes = deltaMinutes;
                bestId = Optional.of(scheduleId);
            }
        }
        return bestId;
    }

    public static boolean recordHasPlaceholderSides(GameResultRecord record) {
        if (record == null) {
            return true;
        }
        GameResultSourceSnapshot source = record.primaryExternalSource();
        if (source == null) {
            return false;
        }
        return FourScorePlayoffPlaceholderNames.isPlaceholder(sideExternalName(source.getHome()))
                || FourScorePlayoffPlaceholderNames.isPlaceholder(sideExternalName(source.getAway()));
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
        if (!isWcBettingSlot(slotId) || records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }
        List<GameResultRecord> filtered = new ArrayList<>();
        for (GameResultRecord record : records) {
            if (recordBelongsToSlot(slotId, record, teamById)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private static boolean kickoffBelongsToSlot(String slotId, LocalDateTime kickoff) {
        return resolveScheduleIdInSlot(slotId, kickoff).isPresent();
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
        if (source != null) {
            String homeName = sideExternalName(source.getHome());
            String awayName = sideExternalName(source.getAway());
            for (ExpectedPair pair : expected) {
                if (sideMatchesFifa(null, homeName, record.getHomeTeamId(), teamById, pair.homeFifa())
                        && sideMatchesFifa(null, awayName, record.getAwayTeamId(), teamById, pair.awayFifa())) {
                    return true;
                }
            }
        }
        return matchesExpectedPairByTeamIds(expected, record, teamById);
    }

    private static boolean matchesExpectedPairByTeamIds(
            Set<ExpectedPair> expected,
            GameResultRecord record,
            Function<String, Optional<Team>> teamById
    ) {
        if (teamById == null
                || record.getHomeTeamId() == null
                || record.getHomeTeamId().isBlank()
                || record.getAwayTeamId() == null
                || record.getAwayTeamId().isBlank()) {
            return false;
        }
        Optional<Team> home = teamById.apply(record.getHomeTeamId());
        Optional<Team> away = teamById.apply(record.getAwayTeamId());
        if (home.isEmpty() || away.isEmpty()) {
            return false;
        }
        for (ExpectedPair pair : expected) {
            if (teamMatchesFifa(home.get(), pair.homeFifa()) && teamMatchesFifa(away.get(), pair.awayFifa())) {
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
