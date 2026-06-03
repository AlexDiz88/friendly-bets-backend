package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Слоты для синхронизации Marathonbet: текущий + 1 следующий.
 */
public final class MarathonbetSyncSlotWindow {

    private static final int ADDITIONAL_SLOTS = 1;

    private MarathonbetSyncSlotWindow() {
    }

    public static List<Integer> resolveSlotOrders(ExternalCompetitionInfoDto info) {
        int current = info.getCurrentMatchday();
        List<ExternalMatchdaySlotDto> slots = info.getMatchdaySlots();

        if (slots != null && !slots.isEmpty()) {
            List<Integer> orders = slots.stream()
                    .map(ExternalMatchdaySlotDto::getValue)
                    .distinct()
                    .sorted()
                    .toList();
            int startIndex = indexOfCurrentSlot(orders, current);
            List<Integer> result = new ArrayList<>();
            int endIndex = Math.min(startIndex + ADDITIONAL_SLOTS, orders.size() - 1);
            for (int i = startIndex; i <= endIndex; i++) {
                result.add(orders.get(i));
            }
            return result;
        }

        int maxOrder = info.getMatchdayCount() > 0 ? info.getMatchdayCount() : current + ADDITIONAL_SLOTS;
        List<Integer> result = new ArrayList<>();
        for (int md = current; md <= Math.min(current + ADDITIONAL_SLOTS, maxOrder); md++) {
            if (md >= 1) {
                result.add(md);
            }
        }
        return result.isEmpty() ? List.of(Math.max(1, current)) : result;
    }

    private static int indexOfCurrentSlot(List<Integer> orders, int current) {
        int idx = orders.indexOf(current);
        if (idx >= 0) {
            return idx;
        }
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) >= current) {
                return i;
            }
        }
        return orders.size() - 1;
    }
}
