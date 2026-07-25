package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;

import java.util.Locale;
import java.util.Set;

public final class TwentyFourScoreLeagueTitles {

    private TwentyFourScoreLeagueTitles() {
    }

    public static boolean matches(League.LeagueCode leagueCode, String competitionTitle) {
        if (leagueCode == null || competitionTitle == null || competitionTitle.isBlank()) {
            return false;
        }
        String title = competitionTitle.toLowerCase(Locale.ROOT);
        return switch (leagueCode) {
            case EPL -> containsAny(title, "премьер", "premier") && containsAny(title, "англия", "england");
            case BL -> containsAny(title, "бундеслига", "bundesliga");
            case CL -> containsAny(title, "лига чемпионов", "champions league");
            case LE -> containsAny(title, "лига европы", "europa league");
            case WC -> containsAny(title, "чемпионат мира", "world cup") && !title.contains("клуб");
            case EC -> containsAny(title, "чемпионат европы", "euro ") || title.contains("евро-");
            default -> false;
        };
    }

    private static boolean containsAny(String title, String... needles) {
        for (String needle : needles) {
            if (title.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static Set<League.LeagueCode> supported() {
        return Set.of(
                League.LeagueCode.EPL,
                League.LeagueCode.BL,
                League.LeagueCode.CL,
                League.LeagueCode.LE,
                League.LeagueCode.WC,
                League.LeagueCode.EC
        );
    }
}
