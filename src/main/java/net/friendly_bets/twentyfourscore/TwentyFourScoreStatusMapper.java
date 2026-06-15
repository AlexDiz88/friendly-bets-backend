package net.friendly_bets.twentyfourscore;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TwentyFourScoreStatusMapper {

    private static final Pattern LIVE_MINUTE = Pattern.compile("(\\d+(?:\\+\\d+)?)\\s*['′]");

    private TwentyFourScoreStatusMapper() {
    }

    public static String mapStatus(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return "SCHEDULED";
        }
        String lower = statusText.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("заверш") || lower.contains("окончен")) {
            return "FINISHED";
        }
        if (lower.contains("ожидается") || lower.contains("не начал")) {
            return "SCHEDULED";
        }
        if (lower.contains("перерыв")) {
            return "IN_PLAY";
        }
        if (lower.contains("идёт") || lower.contains("идет") || LIVE_MINUTE.matcher(lower).find()) {
            return "IN_PLAY";
        }
        if (lower.contains("отмен")) {
            return "CANCELLED";
        }
        if (lower.contains("перенес")) {
            return "POSTPONED";
        }
        return "SCHEDULED";
    }

    public static String extractLiveMinute(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return null;
        }
        Matcher matcher = LIVE_MINUTE.matcher(statusText);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "'";
    }
}
