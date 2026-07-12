package net.friendly_bets.utils;

import net.friendly_bets.models.League;

import java.util.Set;

public final class KnockoutBetPrivacyStages {

    private static final Set<League.LeagueCode> PRIVACY_LEAGUES = Set.of(
            League.LeagueCode.WC,
            League.LeagueCode.EC,
            League.LeagueCode.CL,
            League.LeagueCode.LE
    );

    private static final Set<String> PRIVACY_STAGES = Set.of("1/2", "third_place", "final");

    private KnockoutBetPrivacyStages() {
    }

    public static String normalizeStage(String matchDay) {
        if (matchDay == null || matchDay.isBlank()) {
            return null;
        }
        String trimmed = matchDay.trim();
        if (trimmed.matches(".* \\[\\d+\\]$")) {
            return trimmed.replaceAll(" \\[\\d+\\]$", "");
        }
        if (trimmed.matches(".*-s\\d+$")) {
            return trimmed.replaceAll("-s\\d+$", "");
        }
        return trimmed;
    }

    public static boolean isSensitiveKnockoutSlot(League.LeagueCode leagueCode, String matchDay) {
        if (leagueCode == null || !PRIVACY_LEAGUES.contains(leagueCode)) {
            return false;
        }
        String stage = normalizeStage(matchDay);
        return stage != null && PRIVACY_STAGES.contains(stage);
    }
}
