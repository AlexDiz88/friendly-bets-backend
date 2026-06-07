package net.friendly_bets.marathonbet;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Allowlist моделей Marathonbet из SSE snapshot (см. tools/eag_sse_full.txt, tools/list_marathon_models.py).
 * <p>
 * Тайм/Матч: {@code MTCH_DR} (не {@code MTCH_FTS*} — «первая команда забьёт»).
 * Разница в счёте: {@code MTCH_T1WM}, {@code MTCH_T2WM}, {@code MTCH_TEWM} (не {@code MTCH_SD} — «результативная ничья»).
 */
public final class MarathonbetAllowedMarkets {

    private static final Set<String> EXACT_GOALS = Set.of(
            "MTCH_T12G", "MTCH_T1G", "MTCH_T2G",
            "MTCH_T12G1", "MTCH_T12G2", "MTCH_T12G1OR2",
            "MTCH_T1G1", "MTCH_T1G2", "MTCH_T1G12",
            "MTCH_T2G1", "MTCH_T2G2", "MTCH_T2G12"
    );

    private static final Pattern BTTS_RESULT = Pattern.compile(
            "^MTCH_(T12GW|T12G(?!1|2)\\w*W|T1G12W|T2G12W).*"
    );

    private static final Pattern SCORE_DIFF = Pattern.compile("^MTCH_(T1WM|T2WM|TEWM)$");

    private MarathonbetAllowedMarkets() {
    }

    public static Optional<MarathonbetMarketBucket> bucketFor(String model) {
        if (model == null || !model.startsWith("MTCH_")) {
            return Optional.empty();
        }
        return switch (model) {
            case "MTCH_R" -> Optional.of(MarathonbetMarketBucket.MATCH_RESULT);
            case "MTCH_R1" -> Optional.of(MarathonbetMarketBucket.HALF_TIME_RESULT);
            case "MTCH_R2" -> Optional.of(MarathonbetMarketBucket.SECOND_HALF_RESULT);
            case "MTCH_DC" -> Optional.of(MarathonbetMarketBucket.DOUBLE_CHANCE);
            case "MTCH_DC1" -> Optional.of(MarathonbetMarketBucket.HALF_TIME_DOUBLE_CHANCE);
            case "MTCH_DC2" -> Optional.of(MarathonbetMarketBucket.SECOND_HALF_DOUBLE_CHANCE);
            case "MTCH_HB" -> Optional.of(MarathonbetMarketBucket.HANDICAP);
            case "MTCH_HB1" -> Optional.of(MarathonbetMarketBucket.HALF_TIME_HANDICAP);
            case "MTCH_HB2" -> Optional.of(MarathonbetMarketBucket.SECOND_HALF_HANDICAP);
            case "MTCH_TTLG" -> Optional.of(MarathonbetMarketBucket.TOTALS);
            case "MTCH_TTLG1" -> Optional.of(MarathonbetMarketBucket.HALF_TIME_TOTALS);
            case "MTCH_TTLG2" -> Optional.of(MarathonbetMarketBucket.SECOND_HALF_TOTALS);
            case "MTCH_T1TTLG" -> Optional.of(MarathonbetMarketBucket.TEAM_TOTAL_HOME);
            case "MTCH_T2TTLG" -> Optional.of(MarathonbetMarketBucket.TEAM_TOTAL_AWAY);
            case "MTCH_CSDYN" -> Optional.of(MarathonbetMarketBucket.CORRECT_SCORE);
            case "MTCH_CSW1DYN" -> Optional.of(MarathonbetMarketBucket.FIRST_HALF_CORRECT_SCORE);
            case "MTCH_CSW2DYN" -> Optional.of(MarathonbetMarketBucket.SECOND_HALF_CORRECT_SCORE);
            case "MTCH_T1W0", "MTCH_T2W0", "MTCH_TEW0" -> Optional.of(MarathonbetMarketBucket.CLEAN_WIN);
            case "MTCH_DR" -> Optional.of(MarathonbetMarketBucket.HALF_FULL);
            case "MTCH_R1_R2" -> Optional.of(MarathonbetMarketBucket.FIRST_SECOND_HALF);
            default -> classifyByPattern(model);
        };
    }

    private static Optional<MarathonbetMarketBucket> classifyByPattern(String model) {
        if (MarathonbetResultTotalModels.isFullTimeResultTotal(model)) {
            return Optional.of(MarathonbetMarketBucket.RESULT_TOTAL);
        }
        if (EXACT_GOALS.contains(model)) {
            return Optional.of(MarathonbetMarketBucket.GOALS);
        }
        if (BTTS_RESULT.matcher(model).matches()) {
            return Optional.of(MarathonbetMarketBucket.BTTS_RESULT);
        }
        if (SCORE_DIFF.matcher(model).matches()) {
            return Optional.of(MarathonbetMarketBucket.SCORE_DIFF);
        }
        return Optional.empty();
    }

    public static boolean isAllowedModel(String model) {
        return bucketFor(model).isPresent();
    }
}
