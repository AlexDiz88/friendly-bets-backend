package net.friendly_bets.gameresults;

import net.friendly_bets.models.League;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LeagueCompetitionMapping {

    private static final Map<League.LeagueCode, String> LEAGUE_TO_COMPETITION = new EnumMap<>(League.LeagueCode.class);
    private static final Map<String, League.LeagueCode> COMPETITION_TO_LEAGUE = new HashMap<>();

    static {
        register(League.LeagueCode.EPL, "PL");
        register(League.LeagueCode.BL, "BL1");
        register(League.LeagueCode.CL, "CL");
        register(League.LeagueCode.WC, "WC");
        register(League.LeagueCode.EC, "EC");
    }

    private LeagueCompetitionMapping() {
    }

    private static void register(League.LeagueCode leagueCode, String competitionCode) {
        LEAGUE_TO_COMPETITION.put(leagueCode, competitionCode);
        COMPETITION_TO_LEAGUE.put(competitionCode, leagueCode);
    }

    public static Optional<String> toCompetitionCode(League.LeagueCode leagueCode) {
        return Optional.ofNullable(LEAGUE_TO_COMPETITION.get(leagueCode));
    }

    public static Optional<League.LeagueCode> toLeagueCode(String competitionCode) {
        return Optional.ofNullable(COMPETITION_TO_LEAGUE.get(competitionCode));
    }

    public static boolean isSupported(League.LeagueCode leagueCode) {
        return LEAGUE_TO_COMPETITION.containsKey(leagueCode);
    }

    public static Set<String> allCompetitionCodes() {
        return Set.copyOf(COMPETITION_TO_LEAGUE.keySet());
    }
}
