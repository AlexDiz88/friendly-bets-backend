package net.friendly_bets.sportsru;

/**
 * Resolves sports.ru calendar input for sandbox / HTTP: short tournament slug
 * ({@code premier-league}) → full path. Full paths are left as-is.
 * Must not be used for other providers.
 */
public final class SportsRuCalendarPathSupport {

    private static final String TOURNAMENT_PREFIX = "/football/tournament/";
    private static final String CALENDAR_SUFFIX = "/calendar/";

    private SportsRuCalendarPathSupport() {
    }

    /**
     * @param raw slug ({@code premier-league}) or path ({@code /football/tournament/.../calendar/})
     * @return normalized path starting with {@code /}, or {@code null} if blank/invalid
     */
    public static String resolveCalendarPath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int scheme = value.indexOf("://");
            int slash = value.indexOf('/', scheme + 3);
            if (slash < 0) {
                return null;
            }
            value = value.substring(slash);
        }
        if (value.startsWith("/")) {
            if (!value.endsWith("/")) {
                value = value + "/";
            }
            return value;
        }
        String slug = value;
        while (slug.startsWith("/")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty() || slug.contains("/") || slug.contains("..") || slug.contains(" ")) {
            return null;
        }
        return TOURNAMENT_PREFIX + slug + CALENDAR_SUFFIX;
    }
}
