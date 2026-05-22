package net.friendly_bets.services;

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
        List<ExpandedMatchdaySlot> slots = new ArrayList<>();
        int order = 1;

        if (format.getRegularStage() != null) {
            order = appendRoundRobin(slots, format.getRegularStage(), ExpandedMatchdaySlot.Kind.REGULAR, order);
        }
        if (format.getGroupStage() != null) {
            order = appendRoundRobin(slots, format.getGroupStage(), ExpandedMatchdaySlot.Kind.GROUP, order);
        }
        if (format.getPlayoff() != null && !format.getPlayoff().isEmpty()) {
            order = appendPlayoff(slots, format.getPlayoff(), order);
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

    private int appendPlayoff(List<ExpandedMatchdaySlot> slots, List<PlayoffRound> playoff, int order) {
        for (PlayoffRound stage : playoff) {
            order = appendPlayoffStage(slots, stage, order);
        }
        return order;
    }

    private int appendPlayoffStage(List<ExpandedMatchdaySlot> slots, PlayoffRound round, int order) {
        String stageKey = round.getStage();
        if (stageKey == null || stageKey.isBlank()) {
            throw new BadRequestException("playoffStageRequired");
        }
        int matchdayCount = round.getMatchdayCount();
        if (matchdayCount < 1 || matchdayCount > 2) {
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
                slots.add(ExpandedMatchdaySlot.builder()
                        .id(stageKey + " [" + leg + "]")
                        .order(order++)
                        .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                        .labelKey(stageKey)
                        .build());
            }
        }
        return order;
    }
}
