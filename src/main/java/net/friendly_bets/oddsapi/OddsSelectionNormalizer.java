package net.friendly_bets.oddsapi;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OddsSelectionNormalizer {

    private static final Set<String> DC_SHORTHAND = Set.of("1x", "12", "x2");

    private OddsSelectionNormalizer() {
    }

    public static Optional<OddsSelectionCode> normalize(
            OddsMarketCategory category,
            String rawSelectionKey,
            OddsMatchContext match
    ) {
        if (category == null || rawSelectionKey == null || rawSelectionKey.isBlank()) {
            return Optional.empty();
        }
        String key = rawSelectionKey.trim().toLowerCase(Locale.ROOT);

        return switch (category) {
            case MATCH_RESULT, HALF_TIME_RESULT -> normalizeMatchResult(key);
            case DOUBLE_CHANCE -> normalizeDoubleChance(key, match);
            case HANDICAP -> normalizeSide(key);
            case TOTALS, TEAM_TOTAL_HOME, TEAM_TOTAL_AWAY -> normalizeOverUnder(key);
            case BTTS -> normalizeYesNo(key);
            default -> Optional.empty();
        };
    }

    private static Optional<OddsSelectionCode> normalizeMatchResult(String key) {
        return switch (key) {
            case "home", "1" -> Optional.of(OddsSelectionCode.HOME);
            case "draw", "x" -> Optional.of(OddsSelectionCode.DRAW);
            case "away", "2" -> Optional.of(OddsSelectionCode.AWAY);
            default -> Optional.empty();
        };
    }

    private static Optional<OddsSelectionCode> normalizeDoubleChance(String key, OddsMatchContext match) {
        if (DC_SHORTHAND.contains(key)) {
            return switch (key) {
                case "1x" -> Optional.of(OddsSelectionCode.DC_1X);
                case "12" -> Optional.of(OddsSelectionCode.DC_12);
                case "x2" -> Optional.of(OddsSelectionCode.DC_X2);
                default -> Optional.empty();
            };
        }
        return resolveDoubleChanceFromLabel(key, match);
    }

    static Optional<OddsSelectionCode> resolveDoubleChanceFromLabel(String label, OddsMatchContext match) {
        if (match == null) {
            return Optional.empty();
        }
        String text = label.toLowerCase(Locale.ROOT);
        boolean hasDraw = containsDraw(text);
        boolean hasHome = mentionsTeam(text, match.getHomeTeamName());
        boolean hasAway = mentionsTeam(text, match.getAwayTeamName());

        if (hasHome && hasDraw && !hasAway) {
            return Optional.of(OddsSelectionCode.DC_1X);
        }
        if (hasHome && hasAway && !hasDraw) {
            return Optional.of(OddsSelectionCode.DC_12);
        }
        if (hasDraw && hasAway && !hasHome) {
            return Optional.of(OddsSelectionCode.DC_X2);
        }
        return Optional.empty();
    }

    private static boolean containsDraw(String text) {
        return text.contains("draw") || text.equals("x") || text.contains(" ничья")
                || text.startsWith("x ") || text.endsWith(" x");
    }

    private static boolean mentionsTeam(String text, String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return false;
        }
        if (TeamNameNormalizer.namesMatch(text, teamName)) {
            return true;
        }
        String normalizedTeam = TeamNameNormalizer.normalize(teamName);
        if (normalizedTeam.isEmpty()) {
            return false;
        }
        String[] tokens = normalizedTeam.split("\\s+");
        int matched = 0;
        for (String token : tokens) {
            if (token.length() >= 3 && text.contains(token)) {
                matched++;
            }
        }
        return matched > 0 && matched >= Math.min(2, tokens.length);
    }

    private static Optional<OddsSelectionCode> normalizeSide(String key) {
        return switch (key) {
            case "home" -> Optional.of(OddsSelectionCode.HOME);
            case "away" -> Optional.of(OddsSelectionCode.AWAY);
            default -> Optional.empty();
        };
    }

    private static Optional<OddsSelectionCode> normalizeOverUnder(String key) {
        return switch (key) {
            case "over" -> Optional.of(OddsSelectionCode.OVER);
            case "under" -> Optional.of(OddsSelectionCode.UNDER);
            default -> Optional.empty();
        };
    }

    private static Optional<OddsSelectionCode> normalizeYesNo(String key) {
        return switch (key) {
            case "yes", "y", "да" -> Optional.of(OddsSelectionCode.YES);
            case "no", "n", "нет" -> Optional.of(OddsSelectionCode.NO);
            default -> Optional.empty();
        };
    }

    /** Expand parsed line prices into canonical selection → odds entries. */
    public static void normalizeLinePrices(
            OddsMarketCategory category,
            Map<String, String> rawPrices,
            OddsMatchContext match,
            Map<OddsSelectionCode, String> target
    ) {
        for (Map.Entry<String, String> entry : rawPrices.entrySet()) {
            normalize(category, entry.getKey(), match)
                    .ifPresent(code -> target.put(code, entry.getValue()));
        }
    }
}
