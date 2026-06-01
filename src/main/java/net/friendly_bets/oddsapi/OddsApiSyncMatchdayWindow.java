package net.friendly_bets.oddsapi;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import net.friendly_bets.models.League;

import java.util.ArrayList;
import java.util.List;

/**
 * Какие слоты тура синхронизировать по расписанию odds-api: текущий + N следующих.
 */
public final class OddsApiSyncMatchdayWindow {

    private static final int ADDITIONAL_SLOTS_TOURNAMENT = 3;
    private static final int ADDITIONAL_SLOTS_LEAGUE = 1;

    private OddsApiSyncMatchdayWindow() {
    }

    public static int additionalSlotsAfterCurrent(League.LeagueCode leagueCode) {
        if (leagueCode == League.LeagueCode.WC || leagueCode == League.LeagueCode.EC) {
            return ADDITIONAL_SLOTS_TOURNAMENT;
        }
        return ADDITIONAL_SLOTS_LEAGUE;
    }

    /**
     * Порядковые номера слотов (value / matchday в game_results): текущий и до N следующих.
     */
    public static List<Integer> resolveSlotOrders(ExternalCompetitionInfoDto info, League.LeagueCode leagueCode) {
        int additional = additionalSlotsAfterCurrent(leagueCode);
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
            int endIndex = Math.min(startIndex + additional, orders.size() - 1);
            for (int i = startIndex; i <= endIndex; i++) {
                result.add(orders.get(i));
            }
            return result;
        }

        int maxOrder = info.getMatchdayCount() > 0 ? info.getMatchdayCount() : current + additional;
        List<Integer> result = new ArrayList<>();
        for (int md = current; md <= Math.min(current + additional, maxOrder); md++) {
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
