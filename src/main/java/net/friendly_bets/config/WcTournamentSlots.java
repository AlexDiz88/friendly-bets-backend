package net.friendly_bets.config;

import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.PlayoffRound;

import java.util.ArrayList;
import java.util.List;

/**
 * Betting slots for {@code wc-48teams}: 16 Berlin group slots + playoff sub-slots.
 */
public final class WcTournamentSlots {

    public static final String FORMAT_CODE = "wc-48teams";
    public static final int GROUP_SLOT_COUNT = 16;

    private WcTournamentSlots() {
    }

    public static List<ExpandedMatchdaySlot> expandGroupSlots(int startOrder) {
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = startOrder;
        for (int round = 1; round <= 3; round++) {
            int slotsInRound = round == 3 ? 4 : 6;
            for (int index = 1; index <= slotsInRound; index++) {
                String id = "r" + round + "-s" + index;
                slots.add(ExpandedMatchdaySlot.builder()
                        .id(id)
                        .order(order++)
                        .kind(ExpandedMatchdaySlot.Kind.GROUP)
                        .labelKey(id)
                        .build());
            }
        }
        return slots;
    }

    public static List<ExpandedMatchdaySlot> expandPlayoffSlots(List<PlayoffRound> playoff, int startOrder) {
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = startOrder;
        if (playoff == null) {
            return slots;
        }
        for (PlayoffRound round : playoff) {
            String stage = round.getStage();
            int count = round.getMatchdayCount();
            if (stage == null || stage.isBlank() || count < 1) {
                continue;
            }
            if (count == 1) {
                slots.add(ExpandedMatchdaySlot.builder()
                        .id(stage)
                        .order(order++)
                        .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                        .labelKey(stage)
                        .build());
            } else {
                for (int leg = 1; leg <= count; leg++) {
                    String id = stage + "-s" + leg;
                    slots.add(ExpandedMatchdaySlot.builder()
                            .id(id)
                            .order(order++)
                            .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                            .labelKey(stage)
                            .build());
                }
            }
        }
        return slots;
    }

    /** Schedule ids (1–72) in Berlin betting slot. */
    public static java.util.List<Integer> scheduleIdsForSlot(String slotId) {
        if (slotId == null || !slotId.matches("r[123]-s\\d+")) {
            return java.util.List.of();
        }
        int round = Integer.parseInt(slotId.substring(1, 2));
        int slotIndex = Integer.parseInt(slotId.substring(4));
        int matchesPerSlot = round == 3 ? 6 : 4;
        int startId = (round - 1) * 24 + (slotIndex - 1) * matchesPerSlot + 1;
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; i < matchesPerSlot; i++) {
            ids.add(startId + i);
        }
        return ids;
    }

    /** Bets required in slot (group stage only). */
    public static int betsRequiredForSlot(String slotId) {
        if (slotId == null || !slotId.startsWith("r")) {
            return 1;
        }
        if (slotId.startsWith("r3-")) {
            return 3;
        }
        if (slotId.startsWith("r1-") || slotId.startsWith("r2-")) {
            return 2;
        }
        return 1;
    }
}
