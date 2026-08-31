package net.friendly_bets.championat;

import net.friendly_bets.models.League;

import java.util.Locale;
import java.util.Set;

public final class ChampionatLeagueSupport {

    private ChampionatLeagueSupport() {
    }

    /** LIVE + team-names scope (EPL/BL/CL/LE). */
    public static Set<League.LeagueCode> supported() {
        return Set.of(
                League.LeagueCode.EPL,
                League.LeagueCode.BL,
                League.LeagueCode.CL,
                League.LeagueCode.LE
        );
    }

    public static boolean isSupportedLeagueCode(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return false;
        }
        try {
            return supported().contains(League.LeagueCode.valueOf(leagueCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static League.LeagueCode parseLeagueCode(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return null;
        }
        try {
            League.LeagueCode code = League.LeagueCode.valueOf(leagueCode.trim().toUpperCase(Locale.ROOT));
            return supported().contains(code) ? code : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
