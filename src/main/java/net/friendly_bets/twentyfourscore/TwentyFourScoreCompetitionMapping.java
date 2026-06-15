package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class TwentyFourScoreCompetitionMapping {

    private static final Map<League.LeagueCode, String> LEAGUE_TO_PATH = new EnumMap<>(League.LeagueCode.class);

    static {
        LEAGUE_TO_PATH.put(
                League.LeagueCode.WC,
                "/football/international/fifa_-_world_cup/2026/regular_season/"
        );
    }

    private TwentyFourScoreCompetitionMapping() {
    }

    public static Optional<String> competitionPath(League.LeagueCode leagueCode) {
        return Optional.ofNullable(LEAGUE_TO_PATH.get(leagueCode));
    }

    public static boolean isSupported(League.LeagueCode leagueCode) {
        return LEAGUE_TO_PATH.containsKey(leagueCode);
    }

    public static String worldCupPathMarker() {
        return "fifa_-_world_cup";
    }
}
