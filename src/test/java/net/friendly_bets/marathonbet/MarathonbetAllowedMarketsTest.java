package net.friendly_bets.marathonbet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetAllowedMarketsTest {

    @Test
    void allowsCoreFullTimeMarkets() {
        assertEquals(MarathonbetMarketBucket.MATCH_RESULT, bucket("MTCH_R"));
        assertEquals(MarathonbetMarketBucket.HANDICAP, bucket("MTCH_HB"));
        assertEquals(MarathonbetMarketBucket.TOTALS, bucket("MTCH_TTLG"));
        assertEquals(MarathonbetMarketBucket.CORRECT_SCORE, bucket("MTCH_CSDYN"));
        assertEquals(MarathonbetMarketBucket.FIRST_HALF_CORRECT_SCORE, bucket("MTCH_CSW1DYN"));
        assertEquals(MarathonbetMarketBucket.SECOND_HALF_CORRECT_SCORE, bucket("MTCH_CSW2DYN"));
    }

    @Test
    void routesHalfPeriodResultsSeparatelyFromFullTime() {
        assertEquals(MarathonbetMarketBucket.HALF_TIME_RESULT, bucket("MTCH_R1"));
        assertEquals(MarathonbetMarketBucket.SECOND_HALF_RESULT, bucket("MTCH_R2"));
        assertEquals(MarathonbetMarketBucket.FIRST_SECOND_HALF, bucket("MTCH_R1_R2"));
    }

    @Test
    void allowsGoalsCleanWinAndScoreDiff() {
        assertEquals(MarathonbetMarketBucket.GOALS, bucket("MTCH_T12G"));
        assertEquals(MarathonbetMarketBucket.CLEAN_WIN, bucket("MTCH_T1W0"));
        assertEquals(MarathonbetMarketBucket.SCORE_DIFF, bucket("MTCH_T1WM"));
        assertEquals(MarathonbetMarketBucket.BTTS_RESULT, bucket("MTCH_T12GW1"));
        assertEquals(MarathonbetMarketBucket.BTTS_RESULT, bucket("MTCH_T12GW2"));
        assertEquals(MarathonbetMarketBucket.HALF_FULL, bucket("MTCH_DR"));
    }

    @Test
    void allowsPlayoffKnockoutMarkets() {
        assertEquals(MarathonbetMarketBucket.PLAYOFF, bucket("MTCH_QLF"));
        assertEquals(MarathonbetMarketBucket.PLAYOFF, bucket("MTCH_OT"));
        assertEquals(MarathonbetMarketBucket.PLAYOFF, bucket("MTCH_PNL_SHT"));
        assertEquals(MarathonbetMarketBucket.PLAYOFF, bucket("MTCH_WM_FT1"));
        assertFalse(MarathonbetAllowedMarkets.isAllowedModel("MTCH_PNL"));
        assertFalse(MarathonbetAllowedMarkets.isAllowedModel("MTCH_TEWFB"));
    }

    @Test
    void denyByDefaultForUnknownModels() {
        assertFalse(MarathonbetAllowedMarkets.isAllowedModel("MTCH_TEWFB"));
        assertFalse(MarathonbetAllowedMarkets.isAllowedModel("MTCH_T1NGI"));
        assertFalse(MarathonbetAllowedMarkets.isAllowedModel("MTCH_TBG"));
        assertFalse(MarathonbetAllowedMarkets.bucketFor("MTCH_FTS").isPresent());
    }

    private static MarathonbetMarketBucket bucket(String model) {
        return MarathonbetAllowedMarkets.bucketFor(model).orElseThrow();
    }
}
