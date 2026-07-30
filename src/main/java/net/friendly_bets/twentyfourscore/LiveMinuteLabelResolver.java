package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves canonical live minute labels stored in MongoDB.
 * Stoppage is normalized to {@code 45+}, {@code 90+}, {@code 105+}, {@code 120+}.
 * Apostrophe is added on the frontend for display ({@code 45+'}).
 */
public final class LiveMinuteLabelResolver {

    static final int FIRST_HALF_MIN = 45;
    static final int OT_FIRST_HALF_END_MINUTE = 105;
    static final int OT_SECOND_HALF_END_MINUTE = 120;

    public static final String FIRST_HALF_STOPPAGE_LABEL = "45+";
    public static final String SECOND_HALF_STOPPAGE_LABEL = "90+";
    public static final String OT_FIRST_HALF_STOPPAGE_LABEL = "105+";
    public static final String OT_SECOND_HALF_STOPPAGE_LABEL = "120+";

    /** ~60 min wall clock: first-half stoppage vs second half (e.g. 48'). */
    static final long SECOND_HALF_START_ELAPSED_MIN = 60L;
    /**
     * Regulation ends ~118 min after kickoff (45 + FH stoppage + HT + 45 + SH stoppage).
     * Below this, minute 91–99 is still {@code 90+}, not extra time.
     */
    static final long REGULATION_END_ELAPSED_MIN = 118L;
    /** OT 1st half (~15' + stoppage) ends ~18 min after regulation. */
    static final long OT_SECOND_HALF_START_ELAPSED_MIN = 141L;

    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d{1,3})(?:\\+(\\d{1,2}))?\\s*'?");

    private LiveMinuteLabelResolver() {
    }

    public static String resolve(
            String rawMinuteLabel,
            Instant utcKickoff,
            Instant now,
            String leagueCode,
            String slotId,
            String matchStatus
    ) {
        if (rawMinuteLabel == null || rawMinuteLabel.isBlank()) {
            return null;
        }
        boolean extraTimeAllowed = LiveMatchExtraTimePolicy.extraTimeAllowed(leagueCode, slotId, matchStatus);
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
        return resolvePlainMinute(baseMinute, utcKickoff, now, extraTimeAllowed);
    }

    public static String resolve(
            String rawMinuteLabel,
            Instant utcKickoff,
            Instant now
    ) {
        return resolve(rawMinuteLabel, utcKickoff, now, null, null, null);
    }

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

    static String resolvePlainMinute(
            int apiMinute,
            Instant utcKickoff,
            Instant now,
            boolean extraTimeAllowed
    ) {
        if (apiMinute <= 0) {
            return null;
        }
        long elapsedMin = elapsedMinutes(utcKickoff, now);

        if (apiMinute > FIRST_HALF_MIN && isLikelyFirstHalfStoppage(apiMinute, elapsedMin)) {
            return FIRST_HALF_STOPPAGE_LABEL;
        }

        if (apiMinute > OT_SECOND_HALF_END_MINUTE) {
            return extraTimeAllowed && elapsedMin >= REGULATION_END_ELAPSED_MIN
                    ? OT_SECOND_HALF_STOPPAGE_LABEL
                    : SECOND_HALF_STOPPAGE_LABEL;
        }

        if (apiMinute > OT_FIRST_HALF_END_MINUTE) {
            if (!extraTimeAllowed || elapsedMin < REGULATION_END_ELAPSED_MIN) {
                return SECOND_HALF_STOPPAGE_LABEL;
            }
            if (elapsedMin < OT_SECOND_HALF_START_ELAPSED_MIN) {
                return OT_FIRST_HALF_STOPPAGE_LABEL;
            }
            return apiMinute + "'";
        }

        if (apiMinute > FIRST_HALF_MIN * 2) {
            if (!extraTimeAllowed || elapsedMin < REGULATION_END_ELAPSED_MIN) {
                return SECOND_HALF_STOPPAGE_LABEL;
            }
            return apiMinute + "'";
        }

        return apiMinute + "'";
    }

    static String stoppageLabelForBaseMinute(int baseMinute) {
        if (baseMinute <= FIRST_HALF_MIN) {
            return FIRST_HALF_STOPPAGE_LABEL;
        }
        if (baseMinute <= FIRST_HALF_MIN * 2) {
            return SECOND_HALF_STOPPAGE_LABEL;
        }
        if (baseMinute <= OT_FIRST_HALF_END_MINUTE) {
            return OT_FIRST_HALF_STOPPAGE_LABEL;
        }
        return OT_SECOND_HALF_STOPPAGE_LABEL;
    }

    static boolean isLikelyFirstHalfStoppage(int apiMinute, long elapsedMin) {
        if (apiMinute <= FIRST_HALF_MIN) {
            return false;
        }
        if (elapsedMin <= 0) {
            return apiMinute <= FIRST_HALF_MIN + 5;
        }
        return elapsedMin < SECOND_HALF_START_ELAPSED_MIN;
    }

    private static long elapsedMinutes(Instant utcKickoff, Instant now) {
        if (utcKickoff == null || now == null) {
            return 0L;
        }
        return Math.max(0, Duration.between(utcKickoff, now).toMinutes());
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
