package net.friendly_bets.providers;

import net.friendly_bets.providers.live.LiveMatchSupport;

import java.util.Locale;

/**
 * Maps provider-specific FULL_MATCH status labels to finished.
 * Canonical DB codes reuse {@link LiveMatchSupport}; display strings vary by API.
 */
public final class FullMatchStatusSupport {

    private FullMatchStatusSupport() {
    }

    /**
     * True only when the FULL provider explicitly indicates the match is finished.
     * Empty / unknown / in-play status → not finished (do not finalize or settle).
     */
    public static boolean isProviderFinished(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return false;
        }
        String trimmed = statusText.replace('\u00a0', ' ').trim();
        if (LiveMatchSupport.isFinishedStatus(trimmed)) {
            return true;
        }
        // ё→е so "завершён" matches "завершен"
        String lower = trimmed.toLowerCase(Locale.ROOT).replace('ё', 'е');
        if (lower.startsWith("завершен") || lower.startsWith("finished") || lower.startsWith("ended")) {
            return true;
        }
        // e.g. "Завершен. 7:6 по пенальти" after normalize may stay as "Завершен"
        return false;
    }
}
