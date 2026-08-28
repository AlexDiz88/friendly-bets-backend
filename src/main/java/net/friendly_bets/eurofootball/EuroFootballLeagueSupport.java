package net.friendly_bets.eurofootball;

import net.friendly_bets.models.League;

import java.util.Locale;
import java.util.Set;

public final class EuroFootballLeagueSupport {

    private EuroFootballLeagueSupport() {
    }

    public static Set<League.LeagueCode> supported() {
        return Set.of(
                League.LeagueCode.EPL,
                League.LeagueCode.BL,
                League.LeagueCode.CL,
                League.LeagueCode.LE
        );
    }

    public static boolean isSupportedLeagueCode(String leagueCode) {
        return parseLeagueCode(leagueCode) != null;
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

    /**
     * LIVE JSON tournament identity. {@code slug=premer-liga} is not unique (many countries);
     * EPL/BL require {@code parent_slug}.
     */
    public static boolean matchesTournament(League.LeagueCode leagueCode, String slug, String parentSlug) {
        if (leagueCode == null) {
            return false;
        }
        String s = slug == null ? "" : slug.trim();
        String parent = parentSlug == null ? "" : parentSlug.trim();
        return switch (leagueCode) {
            case EPL -> "premer-liga".equals(s) && "angliya".equals(parent);
            case BL -> "bundesliga".equals(s) && "germaniya".equals(parent);
            case CL -> "liga_chempionov".equals(s);
            case LE -> "liga_evropyi".equals(s);
            default -> false;
        };
    }
}
