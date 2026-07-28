package net.friendly_bets.melbet;

import java.util.Optional;
import java.util.Set;

/**
 * Deny-by-default allowlist of Melbet Digitain StakeType Ids (full-time).
 * Negative companion Ids (−2/−3/−69/−75) are merged into the positive Id before lookup.
 */
public final class MelbetAllowedMarkets {

    private static final Set<Integer> MATCH_RESULT = Set.of(1);
    private static final Set<Integer> DOUBLE_CHANCE = Set.of(37);
    private static final Set<Integer> HANDICAP = Set.of(2);
    private static final Set<Integer> TOTALS = Set.of(3);
    private static final Set<Integer> TEAM_TOTAL_HOME = Set.of(69);
    private static final Set<Integer> TEAM_TOTAL_AWAY = Set.of(75);
    private static final Set<Integer> BTTS = Set.of(26);
    private static final Set<Integer> RESULT_BTTS = Set.of(261_946);
    private static final Set<Integer> RESULT_TOTAL = Set.of(134);
    private static final Set<Integer> HALF_FULL = Set.of(4);
    private static final Set<Integer> FIRST_SECOND_HALF = Set.of(421_317);
    private static final Set<Integer> CORRECT_SCORE = Set.of(682);
    private static final Set<Integer> GOALS = Set.of(27, 28);
    private static final Set<Integer> EXACT_TOTAL_GOALS = Set.of(586, 584, 585);
    private static final Set<Integer> CLEAN_WIN = Set.of(40_393, 40_394);
    private static final Set<Integer> SCORE_DIFF = Set.of(525, 526, 535);

    private MelbetAllowedMarkets() {
    }

    /** Collapse Digitain “main line” companion (−Id) into the positive market Id. */
    public static int canonicalStakeTypeId(int stakeTypeId) {
        return stakeTypeId < 0 ? -stakeTypeId : stakeTypeId;
    }

    public static Optional<MelbetMarketBucket> bucketFor(int stakeTypeId) {
        int id = canonicalStakeTypeId(stakeTypeId);
        if (MATCH_RESULT.contains(id)) {
            return Optional.of(MelbetMarketBucket.MATCH_RESULT);
        }
        if (DOUBLE_CHANCE.contains(id)) {
            return Optional.of(MelbetMarketBucket.DOUBLE_CHANCE);
        }
        if (HANDICAP.contains(id)) {
            return Optional.of(MelbetMarketBucket.HANDICAP);
        }
        if (TOTALS.contains(id)) {
            return Optional.of(MelbetMarketBucket.TOTALS);
        }
        if (TEAM_TOTAL_HOME.contains(id)) {
            return Optional.of(MelbetMarketBucket.TEAM_TOTAL_HOME);
        }
        if (TEAM_TOTAL_AWAY.contains(id)) {
            return Optional.of(MelbetMarketBucket.TEAM_TOTAL_AWAY);
        }
        if (BTTS.contains(id)) {
            return Optional.of(MelbetMarketBucket.BTTS);
        }
        if (RESULT_BTTS.contains(id)) {
            return Optional.of(MelbetMarketBucket.RESULT_BTTS);
        }
        if (RESULT_TOTAL.contains(id)) {
            return Optional.of(MelbetMarketBucket.RESULT_TOTAL);
        }
        if (HALF_FULL.contains(id)) {
            return Optional.of(MelbetMarketBucket.HALF_FULL);
        }
        if (FIRST_SECOND_HALF.contains(id)) {
            return Optional.of(MelbetMarketBucket.FIRST_SECOND_HALF);
        }
        if (CORRECT_SCORE.contains(id)) {
            return Optional.of(MelbetMarketBucket.CORRECT_SCORE);
        }
        if (GOALS.contains(id)) {
            return Optional.of(MelbetMarketBucket.GOALS);
        }
        if (EXACT_TOTAL_GOALS.contains(id)) {
            return Optional.of(MelbetMarketBucket.EXACT_TOTAL_GOALS);
        }
        if (CLEAN_WIN.contains(id)) {
            return Optional.of(MelbetMarketBucket.CLEAN_WIN);
        }
        if (SCORE_DIFF.contains(id)) {
            return Optional.of(MelbetMarketBucket.SCORE_DIFF);
        }
        return Optional.empty();
    }

    public static boolean isAllowed(int stakeTypeId) {
        return bucketFor(stakeTypeId).isPresent();
    }
}
