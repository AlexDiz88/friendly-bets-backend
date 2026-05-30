package net.friendly_bets.wc26;

import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Keeps only matches belonging to a Berlin betting slot ({@code r1-s1} … {@code r3-s4}).
 */
public final class WcBerlinSlotMatchFilter {

    private static final Pattern BERLIN_GROUP_SLOT = Pattern.compile("r[123]-s\\d+");

    private WcBerlinSlotMatchFilter() {
    }

    public static boolean isBerlinGroupSlot(String slotId) {
        return slotId != null && BERLIN_GROUP_SLOT.matcher(slotId).matches();
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
            String homeName = sideExternalName(source.getHome());
            String awayName = sideExternalName(source.getAway());
            if (matchesExpectedPair(expected, null, homeName, null, awayName)) {
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

    private static boolean fifaMatches(String tla, String name, String fifaCode) {
        return Wc26TeamCatalog.nameMatchesFifaCode(name, tla, fifaCode);
    }

    private static String sideExternalName(GameResultSideSnapshot side) {
        return side != null && side.getExternalName() != null ? side.getExternalName().trim() : null;
    }

    private record ExpectedPair(String homeFifa, String awayFifa) {
    }
}
