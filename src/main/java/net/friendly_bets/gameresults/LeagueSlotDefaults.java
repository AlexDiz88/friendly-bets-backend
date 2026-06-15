package net.friendly_bets.gameresults;

import net.friendly_bets.dto.ExternalMatchdaySlotDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LeagueSlotDefaults {

    public record KnockoutSlot(int matchday, String stage, String labelKey, Integer leg) {
        public KnockoutSlot(int matchday, String stage, String labelKey) {
            this(matchday, stage, labelKey, null);
        }
    }

    private static final Map<String, Integer> REGULAR_SEASON_MATCHDAYS = Map.of(
            "PL", 38,
            "BL1", 34,
            "CL", 8,
            "EC", 7,
            "WC", 7
    );

    private static final Map<String, Integer> TOTAL_SLOTS_BY_CODE = Map.of(
            "PL", 38,
            "BL1", 34,
            "CL", 14,
            "EC", 7,
            "WC", 7
    );

    private static final Map<String, List<KnockoutSlot>> KNOCKOUT_SLOTS_BY_CODE = Map.of(
            "CL", List.of(
                    new KnockoutSlot(9, "PLAYOFFS", "1/16 [1]", 1),
                    new KnockoutSlot(10, "PLAYOFFS", "1/16 [2]", 2),
                    new KnockoutSlot(11, "LAST_16", "1/8"),
                    new KnockoutSlot(12, "QUARTER_FINALS", "1/4"),
                    new KnockoutSlot(13, "SEMI_FINALS", "1/2"),
                    new KnockoutSlot(14, "FINAL", "final")
            )
    );

    private LeagueSlotDefaults() {
    }

    public static int regularSeasonMatchdayCount(String competitionCode) {
        return REGULAR_SEASON_MATCHDAYS.getOrDefault(competitionCode, 38);
    }

    public static int totalMatchdaySlots(String competitionCode) {
        return TOTAL_SLOTS_BY_CODE.getOrDefault(competitionCode, 38);
    }

    public static Optional<KnockoutSlot> knockoutSlotForMatchday(String competitionCode, int matchday) {
        List<KnockoutSlot> slots = KNOCKOUT_SLOTS_BY_CODE.get(competitionCode);
        if (slots == null) {
            return Optional.empty();
        }
        return slots.stream()
                .filter(s -> s.matchday() == matchday)
                .findFirst();
    }

    public static List<ExternalMatchdaySlotDto> buildMatchdaySlots(String competitionCode) {
        int regularCount = regularSeasonMatchdayCount(competitionCode);
        List<ExternalMatchdaySlotDto> slots = new ArrayList<>(regularCount);
        for (int i = 1; i <= regularCount; i++) {
            slots.add(ExternalMatchdaySlotDto.builder()
                    .value(i)
                    .slotId(String.valueOf(i))
                    .label(String.valueOf(i))
                    .kind("REGULAR")
                    .build());
        }
        List<KnockoutSlot> knockout = KNOCKOUT_SLOTS_BY_CODE.get(competitionCode);
        if (knockout != null) {
            for (KnockoutSlot slot : knockout) {
                slots.add(ExternalMatchdaySlotDto.builder()
                        .value(slot.matchday())
                        .slotId(slot.labelKey())
                        .label(slot.labelKey())
                        .kind("KNOCKOUT")
                        .build());
            }
        }
        return slots;
    }
}
