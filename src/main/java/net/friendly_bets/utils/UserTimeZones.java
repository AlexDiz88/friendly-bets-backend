package net.friendly_bets.utils;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Curated IANA timezones for user profile preference.
 * Default: {@link #DEFAULT_TIMEZONE} ({@code Europe/Berlin}).
 * <p>
 * One representative per distinct offset ruleset (incl. DST).
 * Sorted ascending by winter then summer UTC offset.
 */
@UtilityClass
public class UserTimeZones {

    public static final String DEFAULT_TIMEZONE = "Europe/Berlin";

    /**
     * Whitelist sorted by UTC offset ascending (winter, then summer).
     * No aliases — only these IDs are accepted.
     */
    public static final List<String> SUPPORTED_TIMEZONES = List.of(
            "America/Los_Angeles",   // UTC-8 / UTC-7
            "America/Denver",        // UTC-7 / UTC-6
            "America/Chicago",       // UTC-6 / UTC-5
            "America/Mexico_City",   // UTC-6 (no DST)
            "America/New_York",      // UTC-5 / UTC-4
            "America/Sao_Paulo",     // UTC-3 (no DST)
            "Atlantic/Reykjavik",    // UTC+0 (no DST)
            "Europe/London",         // UTC+0 / UTC+1
            "Europe/Berlin",         // UTC+1 / UTC+2 (default)
            "Europe/Kyiv",           // UTC+2 / UTC+3
            "Europe/Moscow",         // UTC+3 (no DST)
            "Asia/Tbilisi",          // UTC+4 (no DST)
            "Asia/Yekaterinburg",    // UTC+5 (no DST)
            "Asia/Kolkata",          // UTC+5:30
            "Asia/Novosibirsk",      // UTC+7
            "Asia/Shanghai",         // UTC+8
            "Asia/Tokyo",            // UTC+9
            "Asia/Vladivostok",      // UTC+10
            "Australia/Sydney",      // UTC+10 / UTC+11
            "Pacific/Auckland"       // UTC+12 / UTC+13
    );

    private static final Set<String> SUPPORTED_SET = new LinkedHashSet<>(SUPPORTED_TIMEZONES);

    public static boolean isSupported(String timezone) {
        return timezone != null && SUPPORTED_SET.contains(timezone.trim());
    }

    /** Null/blank only → {@link #DEFAULT_TIMEZONE}. Does not remap unknown IDs. */
    public static String defaultIfBlank(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        return timezone.trim();
    }
}
