package net.friendly_bets.externaldata.footballdata;

import net.friendly_bets.models.ExpandedMatchdaySlot;

/**
 * Maps FriendlyBets betting slot ({@code order} / {@code id}) to football-data.org {@code matchday} parameter.
 */
public final class FootballDataBettingSlotApiMatchdayResolver {

    /** World Cup: 6 betting slots per official group-stage matchday (18 slots → API 1..3). */
    public static final String WC_COMPETITION_CODE = "WC";
    public static final int WC_BETTING_SLOTS_PER_OFFICIAL_MATCHDAY = 6;
    public static final int WC_GROUP_BETTING_SLOT_COUNT = 18;

    private FootballDataBettingSlotApiMatchdayResolver() {
    }

    public static int resolveApiMatchday(String competitionCode, ExpandedMatchdaySlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot is null");
        }
        if (isWcGroupedBettingSlot(competitionCode, slot)) {
            return wcApiMatchdayFromOrder(slot.getOrder());
        }
        return Integer.parseInt(slot.getId().trim());
    }

    public static boolean isWcGroupedBettingSlot(String competitionCode, ExpandedMatchdaySlot slot) {
        if (!WC_COMPETITION_CODE.equals(competitionCode)) {
            return false;
        }
        if (slot.getKind() != ExpandedMatchdaySlot.Kind.GROUP && slot.getKind() != ExpandedMatchdaySlot.Kind.REGULAR) {
            return false;
        }
        int order = slot.getOrder();
        return order >= 1 && order <= WC_GROUP_BETTING_SLOT_COUNT;
    }

    /** Slots 1..6 → 1, 7..12 → 2, 13..18 → 3. */
    public static int wcApiMatchdayFromOrder(int bettingSlotOrder) {
        if (bettingSlotOrder < 1 || bettingSlotOrder > WC_GROUP_BETTING_SLOT_COUNT) {
            throw new IllegalArgumentException("WC betting slot order out of range: " + bettingSlotOrder);
        }
        return (bettingSlotOrder - 1) / WC_BETTING_SLOTS_PER_OFFICIAL_MATCHDAY + 1;
    }
}
