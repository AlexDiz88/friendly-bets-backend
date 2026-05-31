package net.friendly_bets.services;

import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.PlayoffRound;
import net.friendly_bets.models.RoundRobinStage;
import net.friendly_bets.models.TournamentFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TournamentFormatExpander {

    public List<ExpandedMatchdaySlot> expand(TournamentFormat format) {
        if (format == null) {
            throw new BadRequestException("tournamentFormatIsNull");
        }
        if (WcTournamentSlots.FORMAT_CODE.equals(format.getFormatCode())) {
            return expandWc(format);
        }
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = 1;

        if (format.getRegularStage() != null) {
            order = appendRoundRobin(slots, format.getRegularStage(), ExpandedMatchdaySlot.Kind.REGULAR, order);
        }
        if (format.getGroupStage() != null) {
            order = appendRoundRobin(slots, format.getGroupStage(), ExpandedMatchdaySlot.Kind.GROUP, order);
        }
        if (format.getPlayoff() != null && !format.getPlayoff().isEmpty()) {
            order = appendPlayoff(slots, format.getPlayoff(), order, false);
        }

        if (slots.isEmpty()) {
            throw new BadRequestException("tournamentFormatHasNoStages");
        }
        return slots;
    }

    private List<ExpandedMatchdaySlot> expandWc(TournamentFormat format) {
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = 1;
        if (format.getGroupStage() != null && format.getGroupStage().isSplitSlotsPerRound()) {
            order = appendRoundRobin(slots, format.getGroupStage(), ExpandedMatchdaySlot.Kind.GROUP, order);
        } else {
            slots.addAll(WcTournamentSlots.expandGroupSlots(1));
            order = slots.size() + 1;
        }
        if (format.getPlayoff() != null && !format.getPlayoff().isEmpty()) {
            slots.addAll(WcTournamentSlots.expandPlayoffSlots(format.getPlayoff(), order));
        }
        if (slots.isEmpty()) {
            throw new BadRequestException("tournamentFormatHasNoStages");
        }
        return slots;
    }

    public Optional<ExpandedMatchdaySlot> findByOrder(TournamentFormat format, int order) {
        return expand(format).stream()
                .filter(s -> s.getOrder() == order)
                .findFirst();
    }

    public Optional<ExpandedMatchdaySlot> findBySlotId(TournamentFormat format, String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return Optional.empty();
        }
        return expand(format).stream()
                .filter(s -> slotId.equals(s.getId()))
                .findFirst();
    }

    public Optional<Integer> resolveOrder(TournamentFormat format, String matchDay) {
        if (matchDay == null || matchDay.isBlank()) {
            return Optional.empty();
        }
        return findBySlotId(format, matchDay.trim()).map(ExpandedMatchdaySlot::getOrder);
    }

    private int appendRoundRobin(
            List<ExpandedMatchdaySlot> slots,
            RoundRobinStage stage,
            ExpandedMatchdaySlot.Kind kind,
            int order
    ) {
        if (stage.getMatchdayCount() < 1) {
            throw new BadRequestException("invalidMatchdayCount");
        }
        if (kind == ExpandedMatchdaySlot.Kind.GROUP && stage.isSplitSlotsPerRound()) {
            return appendSplitGroupSlots(slots, stage, order);
        }
        for (int i = 1; i <= stage.getMatchdayCount(); i++) {
            slots.add(ExpandedMatchdaySlot.builder()
                    .id(String.valueOf(i))
                    .order(order++)
                    .kind(kind)
                    .labelKey(String.valueOf(i))
                    .build());
        }
        return order;
    }

    private int appendSplitGroupSlots(
            List<ExpandedMatchdaySlot> slots,
            RoundRobinStage stage,
            int order
    ) {
        validateSplitGroupStage(stage);
        for (int round = 1; round <= stage.getMatchdayCount(); round++) {
            int slotsInRound = stage.getSlotsPerRound().get(round - 1);
            for (int slotIndex = 1; slotIndex <= slotsInRound; slotIndex++) {
                String id = round + " [" + slotIndex + "]";
                slots.add(ExpandedMatchdaySlot.builder()
                        .id(id)
                        .order(order++)
                        .kind(ExpandedMatchdaySlot.Kind.GROUP)
                        .labelKey(id)
                        .build());
            }
        }
        return order;
    }

    private void validateSplitGroupStage(RoundRobinStage stage) {
        List<Integer> perRound = stage.getSlotsPerRound();
        if (perRound == null || perRound.size() != stage.getMatchdayCount()) {
            throw new BadRequestException("groupSlotsPerRoundSizeMismatch");
        }
        for (Integer count : perRound) {
            if (count == null || count < 1 || count > 8) {
                throw new BadRequestException("invalidGroupSlotCount");
            }
        }
    }

    private int appendPlayoff(List<ExpandedMatchdaySlot> slots, List<PlayoffRound> playoff, int order, boolean wcStyleIds) {
        for (PlayoffRound stage : playoff) {
            order = appendPlayoffStage(slots, stage, order, wcStyleIds);
        }
        return order;
    }

    private int appendPlayoffStage(List<ExpandedMatchdaySlot> slots, PlayoffRound round, int order, boolean wcStyleIds) {
        String stageKey = round.getStage();
        if (stageKey == null || stageKey.isBlank()) {
            throw new BadRequestException("playoffStageRequired");
        }
        int matchdayCount = round.getMatchdayCount();
        int maxLegs = wcStyleIds ? 8 : 2;
        if (matchdayCount < 1 || matchdayCount > maxLegs) {
            throw new BadRequestException("invalidPlayoffMatchdayCount");
        }
        if (matchdayCount == 1) {
            slots.add(ExpandedMatchdaySlot.builder()
                    .id(stageKey)
                    .order(order++)
                    .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                    .labelKey(stageKey)
                    .build());
        } else {
            for (int leg = 1; leg <= matchdayCount; leg++) {
                String id = wcStyleIds ? stageKey + "-s" + leg : stageKey + " [" + leg + "]";
                slots.add(ExpandedMatchdaySlot.builder()
                        .id(id)
                        .order(order++)
                        .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                        .labelKey(stageKey)
                        .build());
            }
        }
        return order;
    }
}
