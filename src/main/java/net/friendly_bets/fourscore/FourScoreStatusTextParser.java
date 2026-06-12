package net.friendly_bets.fourscore;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор текста статуса 4score (напр. «Идёт 72'», «Перерыв», «Завершено»).
 */
public final class FourScoreStatusTextParser {

    private static final Pattern MINUTE_LABEL = Pattern.compile("(\\d+(?:\\+\\d+)?)\\s*['′]");

    public record ParsedStatus(String mappedStatus, String liveMinuteLabel) {
    }

    private FourScoreStatusTextParser() {
    }

    public static ParsedStatus parse(String statusText) {
        String mapped = FourScoreScoreNormalizer.mapStatus(statusText);
        String minuteLabel = extractLiveMinuteLabel(statusText, mapped);
        return new ParsedStatus(mapped, minuteLabel);
    }

    private static String extractLiveMinuteLabel(String statusText, String mappedStatus) {
        if (statusText == null || statusText.isBlank() || !"IN_PLAY".equals(mappedStatus)) {
            return null;
        }
        Matcher matcher = MINUTE_LABEL.matcher(statusText);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "'";
    }

    /** Матч на list/event странице требует загрузки event page (live или завершён). */
    public static boolean needsEventDetails(String statusText, Integer homeScore, Integer awayScore) {
        if (statusText != null && !statusText.isBlank()) {
            String lower = statusText.trim().toLowerCase(Locale.ROOT);
            if (lower.contains("завершено")) {
                return true;
            }
            if (lower.contains("не началось")) {
                return false;
            }
            if (lower.contains("идёт") || lower.contains("идет") || lower.contains("перерыв")) {
                return true;
            }
        }
        return homeScore != null
                && awayScore != null
                && (homeScore > 0 || awayScore > 0);
    }
}
