package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.ExpandedMatchdaySlot;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandedMatchdaySlotDto {

    private String id;
    private int order;
    private String kind;
    private String labelKey;

    public static ExpandedMatchdaySlotDto from(ExpandedMatchdaySlot slot) {
        return ExpandedMatchdaySlotDto.builder()
                .id(slot.getId())
                .order(slot.getOrder())
                .kind(slot.getKind().name())
                .labelKey(slot.getLabelKey())
                .build();
    }
}
