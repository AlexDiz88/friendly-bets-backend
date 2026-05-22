package net.friendly_bets.footballdata;

import java.util.Map;

public final class FootballDataCompetitionDefaults {

    private static final Map<String, Integer> MATCHDAY_COUNT_BY_CODE = Map.of(
            "PL", 38,
            "BL1", 34,
            "CL", 13,
            "EC", 7,
            "WC", 7
    );

    private FootballDataCompetitionDefaults() {
    }

    public static int defaultMatchdayCount(String competitionCode) {
        return MATCHDAY_COUNT_BY_CODE.getOrDefault(competitionCode, 38);
    }
}
