package net.friendly_bets.footballdata;

import net.friendly_bets.dto.ExternalMatchdaySlotDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Синтетические номера туров для стадий плей-офф (запрос к API по {@code stage}, не по {@code matchday}).
 */
public final class FootballDataKnockoutMatchdays {

    private record KnockoutSlot(int matchday, String stage, String labelKey) {
    }

    private static final Map<String, List<KnockoutSlot>> KNOCKOUT_SLOTS_BY_CODE = Map.of(
            "CL", List.of(
                    new KnockoutSlot(9, "LAST_16", "1/8"),
                    new KnockoutSlot(10, "QUARTER_FINALS", "1/4"),
                    new KnockoutSlot(11, "SEMI_FINALS", "1/2"),
                    new KnockoutSlot(12, "FINAL", "final")
            )
    );

    private FootballDataKnockoutMatchdays() {
    }

    public static Optional<String> stageForMatchday(String competitionCode, int matchday) {
        List<KnockoutSlot> slots = KNOCKOUT_SLOTS_BY_CODE.get(competitionCode);
        if (slots == null) {
            return Optional.empty();
        }
        return slots.stream()
                .filter(s -> s.matchday() == matchday)
                .map(KnockoutSlot::stage)
                .findFirst();
    }

    public static boolean isKnockoutMatchday(String competitionCode, int matchday) {
        return stageForMatchday(competitionCode, matchday).isPresent();
    }

    public static List<ExternalMatchdaySlotDto> buildMatchdaySlots(String competitionCode) {
        int regularCount = FootballDataCompetitionDefaults.regularSeasonMatchdayCount(competitionCode);
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
