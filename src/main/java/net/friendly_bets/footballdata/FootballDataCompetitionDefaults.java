package net.friendly_bets.footballdata;

import java.util.Map;

public final class FootballDataCompetitionDefaults {

    private static final Map<String, Integer> REGULAR_SEASON_MATCHDAYS = Map.of(
            "PL", 38,
            "BL1", 34,
            "CL", 8,
            "EC", 7,
            "WC", 7
    );

    private static final Map<String, Integer> TOTAL_SLOTS_BY_CODE = Map.of(
            "PL", 38,
            "BL1", 34,
            "CL", 12,
            "EC", 7,
            "WC", 7
    );

    private FootballDataCompetitionDefaults() {
    }

    public static int regularSeasonMatchdayCount(String competitionCode) {
        return REGULAR_SEASON_MATCHDAYS.getOrDefault(competitionCode, 38);
    }

    public static int totalMatchdaySlots(String competitionCode) {
        return TOTAL_SLOTS_BY_CODE.getOrDefault(competitionCode, 38);
    }

    /** @deprecated use {@link #totalMatchdaySlots} */
    public static int defaultMatchdayCount(String competitionCode) {
        return totalMatchdaySlots(competitionCode);
    }
}
