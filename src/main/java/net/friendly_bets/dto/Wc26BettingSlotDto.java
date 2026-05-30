package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26BettingSlotDto {

    /** r1-s1, r2-s3, … */
    private String id;
    private int round;
    private int slotIndex;
    private int betsRequired;
    private int matchesPerSlot;
    private List<ExternalMatchDto> matches;
}
