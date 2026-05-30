package net.friendly_bets.utils;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Team;

import java.util.Locale;

public final class TeamTitleUtils {

    public static final String FOOTBALL_DATA_PROVIDER = "football-data";
    /** {@link net.friendly_bets.gameresults.MatchDataProviders#ODDS_API} */
    public static final String ODDS_API_PROVIDER = "odds-api.io";
    public static final String WC26_PROVIDER = "wc26";

    private TeamTitleUtils() {
    }

    /** PascalCase без пробелов — ключ i18n и имя файла {@code /upload/logo/{lowercase}.png}. */
    public static String normalizeTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("teamTitleRequired");
        }
        String title = raw.trim();
        if (!title.matches("[A-Za-z][A-Za-z0-9]*")) {
            throw new BadRequestException("teamTitleInvalid");
        }
        return title;
    }

    public static String toLocalLogoFileKey(String title) {
        return title.toLowerCase(Locale.ROOT);
    }

    public static String effectiveTitle(Team team) {
        if (team == null || team.getTitle() == null) {
            return "";
        }
        return team.getTitle().trim();
    }
}
