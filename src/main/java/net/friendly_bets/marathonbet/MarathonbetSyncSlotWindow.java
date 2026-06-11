package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Слоты для синхронизации Marathonbet: текущий и/или следующий тур.
 */
public final class MarathonbetSyncSlotWindow {

    private static final int ADDITIONAL_SLOTS = 1;

    private MarathonbetSyncSlotWindow() {
    }

    public static List<Integer> resolveSlotOrders(ExternalCompetitionInfoDto info) {
        return resolveSlotOrders(info, MarathonbetSlotScope.BOTH);
    }

    public static List<Integer> resolveSlotOrders(ExternalCompetitionInfoDto info, MarathonbetSlotScope scope) {
        List<Integer> window = resolveCurrentAndNext(info);
        return switch (scope) {
            case CURRENT -> window.isEmpty() ? List.of() : List.of(window.get(0));
            case NEXT -> window.size() > 1 ? List.of(window.get(1)) : List.of();
            case BOTH -> window;
        };
    }

    private static List<Integer> resolveCurrentAndNext(ExternalCompetitionInfoDto info) {
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
