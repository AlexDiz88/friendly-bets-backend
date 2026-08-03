package net.friendly_bets.providers.odds;

import java.util.List;

/**
 * Cron-only ODDS slot decision for one league (current vs next vs skip).
 */
public record OddsCronSlotPlan(
        boolean skip,
        OddsSlotScope scope,
        List<Integer> slotOrders,
        OddsFetchPolicy fetchPolicy,
        String reason
) {
    public static OddsCronSlotPlan skip(String reason) {
        return new OddsCronSlotPlan(true, null, List.of(), null, reason);
    }

    public static OddsCronSlotPlan sync(
            OddsSlotScope scope,
            List<Integer> slotOrders,
            OddsFetchPolicy fetchPolicy,
            String reason
    ) {
        return new OddsCronSlotPlan(
                false,
                scope,
                slotOrders == null ? List.of() : List.copyOf(slotOrders),
                fetchPolicy,
                reason
        );
    }
}
