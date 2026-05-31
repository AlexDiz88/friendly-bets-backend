package net.friendly_bets.config;

import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.PlayoffRound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Betting slots for {@code wc-48teams}: 16 Berlin group slots + playoff sub-slots.
 * Canonical {@code Bet.match_day} ids: {@code 1 [1]} … {@code 3 [4]}, {@code 1/16 [1]}, …
 */
public final class WcTournamentSlots {

    public static final String FORMAT_CODE = "wc-48teams";
    public static final int GROUP_SLOT_COUNT = 16;

    private static final Pattern BERLIN_GROUP_SLOT = Pattern.compile("^([123]) \\[(\\d+)\\]$");

    private WcTournamentSlots() {
    }

    public static String groupSlotId(int round, int index) {
        return round + " [" + index + "]";
    }

    public static String playoffSlotId(String stage, int leg) {
        return stage + " [" + leg + "]";
    }

    public static boolean isBerlinGroupSlot(String slotId) {
        return slotId != null && BERLIN_GROUP_SLOT.matcher(slotId).matches();
    }

    public static Optional<int[]> parseBerlinGroupSlot(String slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        Matcher matcher = BERLIN_GROUP_SLOT.matcher(slotId);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new int[]{
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        });
    }

    public static List<ExpandedMatchdaySlot> expandGroupSlots(int startOrder) {
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = startOrder;
        for (int round = 1; round <= 3; round++) {
            int slotsInRound = round == 3 ? 4 : 6;
            for (int index = 1; index <= slotsInRound; index++) {
                String id = groupSlotId(round, index);
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
                    String id = playoffSlotId(stage, leg);
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
    public static List<Integer> scheduleIdsForSlot(String slotId) {
        Optional<int[]> parsed = parseBerlinGroupSlot(slotId);
        if (parsed.isEmpty()) {
            return List.of();
        }
        int round = parsed.get()[0];
        int slotIndex = parsed.get()[1];
        int matchesPerSlot = round == 3 ? 6 : 4;
        int startId = (round - 1) * 24 + (slotIndex - 1) * matchesPerSlot + 1;
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < matchesPerSlot; i++) {
            ids.add(startId + i);
        }
        return ids;
    }

    /** Bets required in slot (group stage only). */
    public static int betsRequiredForSlot(String slotId) {
        Optional<int[]> parsed = parseBerlinGroupSlot(slotId);
        if (parsed.isEmpty()) {
            return 1;
        }
        int round = parsed.get()[0];
        if (round == 3) {
            return 3;
        }
        if (round == 1 || round == 2) {
            return 2;
        }
        return 1;
    }
}
