package net.friendly_bets.fourscore;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * TBD sides on 4score playoff list pages (e.g. {@code 2nd Group I}, {@code 2-я группа Д}).
 */
public final class FourScorePlayoffPlaceholderNames {

    private static final Pattern EN_GROUP_PLACEHOLDER = Pattern.compile(
            "^\\d+(?:st|nd|rd|th)\\s+group\\s+[a-z]$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern RU_GROUP_PLACEHOLDER = Pattern.compile(
            "^\\d+-я\\s+группа\\s+[а-яa-z]$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern EN_WINNER_LOSER = Pattern.compile(
            "^(?:winner|loser)\\s+(?:match\\s+)?\\d+$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern RU_WINNER_LOSER = Pattern.compile(
            "^(?:победитель|проигравший)\\s+матча\\s+\\d+$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private FourScorePlayoffPlaceholderNames() {
    }

    public static boolean isPlaceholder(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return false;
        }
        String trimmed = teamName.trim();
        return EN_GROUP_PLACEHOLDER.matcher(trimmed).matches()
                || RU_GROUP_PLACEHOLDER.matcher(trimmed).matches()
                || EN_WINNER_LOSER.matcher(trimmed).matches()
                || RU_WINNER_LOSER.matcher(trimmed).matches()
                || isGroupOrdinalOnly(trimmed);
    }

    private static boolean isGroupOrdinalOnly(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches("^\\d+(?:st|nd|rd|th)?\\s+group\\s+[a-z]$")
                || lower.matches("^\\d+-я\\s+группа\\s+[а-яa-z]$");
    }
}
