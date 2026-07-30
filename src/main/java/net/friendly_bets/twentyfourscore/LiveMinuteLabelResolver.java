package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves display labels for live match minutes from provider data and kickoff time.
 * Stoppage is always shown as {@code 45+} or {@code 90+}, regardless of raw API format.
 */
public final class LiveMinuteLabelResolver {

    static final int FIRST_HALF_MIN = 45;
    public static final String FIRST_HALF_STOPPAGE_LABEL = "45+";
    public static final String SECOND_HALF_STOPPAGE_LABEL = "90+";
    /** Second half typically starts ~60 min after UTC kickoff (45' + stoppage + short break). */
    static final long SECOND_HALF_START_ELAPSED_MIN = 60L;
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d{1,3})(?:\\+(\\d{1,2}))?\\s*'?");

    private LiveMinuteLabelResolver() {
    }

    /**
     * @param rawMinuteLabel value from provider HTML (e.g. {@code 48'}, {@code 90+2'})
     * @param utcKickoff     match kickoff; may be null
     * @param now            reference instant for elapsed-time half detection
     */
    public static String resolve(String rawMinuteLabel, Instant utcKickoff, Instant now) {
        if (rawMinuteLabel == null || rawMinuteLabel.isBlank()) {
            return null;
        }
        String trimmed = rawMinuteLabel.trim();
        Matcher matcher = MINUTE_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return trimmed.endsWith("'") ? trimmed : trimmed + "'";
        }
        int baseMinute = Integer.parseInt(matcher.group(1));
        String addedPart = matcher.group(2);
        if (addedPart != null || trimmed.contains("+")) {
            return stoppageLabelForBaseMinute(baseMinute);
        }
        return resolvePlainMinute(baseMinute, utcKickoff, now);
    }

    /**
     * @return match minute as integer for storage, or null if not parseable
     */
    public static Integer parseMinuteInteger(String rawMinuteLabel) {
        if (rawMinuteLabel == null || rawMinuteLabel.isBlank()) {
            return null;
        }
        Matcher matcher = MINUTE_PATTERN.matcher(rawMinuteLabel.trim());
        if (!matcher.find()) {
            return null;
        }
        int base = Integer.parseInt(matcher.group(1));
        String added = matcher.group(2);
        if (added != null) {
            return base + Integer.parseInt(added);
        }
        return base;
    }

    static String resolvePlainMinute(int apiMinute, Instant utcKickoff, Instant now) {
        if (apiMinute <= 0) {
            return null;
        }
        if (apiMinute > FIRST_HALF_MIN && isLikelyFirstHalfStoppage(apiMinute, utcKickoff, now)) {
            return FIRST_HALF_STOPPAGE_LABEL;
        }
        if (apiMinute > FIRST_HALF_MIN * 2) {
            return SECOND_HALF_STOPPAGE_LABEL;
        }
        return apiMinute + "'";
    }

    static String stoppageLabelForBaseMinute(int baseMinute) {
        if (baseMinute <= FIRST_HALF_MIN) {
            return FIRST_HALF_STOPPAGE_LABEL;
        }
        return SECOND_HALF_STOPPAGE_LABEL;
    }

    static boolean isLikelyFirstHalfStoppage(int apiMinute, Instant utcKickoff, Instant now) {
        if (apiMinute <= FIRST_HALF_MIN) {
            return false;
        }
        if (utcKickoff == null || now == null) {
            return apiMinute <= FIRST_HALF_MIN + 5;
        }
        long elapsedMin = Math.max(0, Duration.between(utcKickoff, now).toMinutes());
        return elapsedMin < SECOND_HALF_START_ELAPSED_MIN;
    }

    static boolean isSupportedLeagueCode(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return false;
        }
        try {
            return TwentyFourScoreLeagueTitles.supported()
                    .contains(League.LeagueCode.valueOf(leagueCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
