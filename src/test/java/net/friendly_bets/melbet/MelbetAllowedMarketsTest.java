package net.friendly_bets.melbet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MelbetAllowedMarketsTest {

    @Test
    void mergesNegativeCompanionIds() {
        assertThat(MelbetAllowedMarkets.canonicalStakeTypeId(-2)).isEqualTo(2);
        assertThat(MelbetAllowedMarkets.bucketFor(-2)).contains(MelbetMarketBucket.HANDICAP);
        assertThat(MelbetAllowedMarkets.bucketFor(2)).contains(MelbetMarketBucket.HANDICAP);
    }

    @Test
    void deniesAsianAndUnknown() {
        assertThat(MelbetAllowedMarkets.isAllowed(2532)).isFalse();
        assertThat(MelbetAllowedMarkets.isAllowed(618)).isFalse();
        assertThat(MelbetAllowedMarkets.isAllowed(999_999)).isFalse();
    }

    @Test
    void allowsCoreMarkets() {
        assertThat(MelbetAllowedMarkets.bucketFor(1)).contains(MelbetMarketBucket.MATCH_RESULT);
        assertThat(MelbetAllowedMarkets.bucketFor(37)).contains(MelbetMarketBucket.DOUBLE_CHANCE);
        assertThat(MelbetAllowedMarkets.bucketFor(26)).contains(MelbetMarketBucket.BTTS);
        assertThat(MelbetAllowedMarkets.bucketFor(682)).contains(MelbetMarketBucket.CORRECT_SCORE);
    }
}
