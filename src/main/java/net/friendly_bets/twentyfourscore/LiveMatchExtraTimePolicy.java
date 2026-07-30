package net.friendly_bets.twentyfourscore;

import net.friendly_bets.config.WcTournamentSlots;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Whether a match can go to extra time / penalties (knockout cups only).
 */
public final class LiveMatchExtraTimePolicy {

    private static final Set<String> NEVER_EXTRA_TIME = Set.of("EPL", "BL");
    private static final Set<String> MAY_EXTRA_TIME = Set.of("CL", "LE", "WC", "EC");
    private static final Pattern NUMERIC_SLOT = Pattern.compile("^\\d+$");
    private static final Pattern WC_GROUP_SLOT = Pattern.compile("^\\d+ \\[\\d+\\]$");
    private static final Pattern KNOCKOUT_STAGE = Pattern.compile(
            "^(1/\\d+|1/8|1/4|1/2|final|third_place|round_of_\\d+).*",
            Pattern.CASE_INSENSITIVE
    );

    private LiveMatchExtraTimePolicy() {
    }

    public static boolean extraTimeAllowed(String leagueCode, String slotId, String matchStatus) {
        if (isExtraTimeStatus(matchStatus)) {
            return true;
        }
        if (leagueCode == null || leagueCode.isBlank()) {
            return false;
        }
        String code = leagueCode.trim().toUpperCase(Locale.ROOT);
        if (NEVER_EXTRA_TIME.contains(code)) {
            return false;
        }
        if (!MAY_EXTRA_TIME.contains(code)) {
            return false;
        }
        return isKnockoutSlot(slotId);
    }

    public static boolean isExtraTimeStatus(String matchStatus) {
        if (matchStatus == null || matchStatus.isBlank()) {
            return false;
        }
        String normalized = matchStatus.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("EXTRA_TIME")
                || normalized.equals("AET")
                || normalized.equals("PENALTY_SHOOTOUT");
    }

    static boolean isKnockoutSlot(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return false;
        }
        String id = normalizeSlotId(slotId);
        if (NUMERIC_SLOT.matcher(id).matches()) {
            return false;
        }
        if (WC_GROUP_SLOT.matcher(id).matches()) {
            return false;
        }
        if (WcTournamentSlots.isPlayoffSlot(id)) {
            return true;
        }
        return KNOCKOUT_STAGE.matcher(id).matches();
    }

    private static String normalizeSlotId(String slotId) {
        String trimmed = slotId.trim();
        if (trimmed.matches(".* \\[\\d+\\]$")) {
            return trimmed.replaceAll(" \\[\\d+\\]$", "");
        }
        if (trimmed.matches(".*-s\\d+$")) {
            return trimmed.replaceAll("-s\\d+$", "");
        }
        return trimmed;
    }
}
