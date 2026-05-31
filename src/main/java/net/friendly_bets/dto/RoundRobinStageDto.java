package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.RoundRobinStage;

import javax.validation.constraints.Min;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundRobinStageDto {

    @Min(1)
    private int matchdayCount;

    private Boolean splitSlotsPerRound;

    private List<@Min(1) Integer> slotsPerRound;

    public static RoundRobinStageDto from(RoundRobinStage stage) {
        if (stage == null) {
            return null;
        }
        return RoundRobinStageDto.builder()
                .matchdayCount(stage.getMatchdayCount())
                .splitSlotsPerRound(stage.getSplitSlotsPerRound())
                .slotsPerRound(stage.getSlotsPerRound())
                .build();
    }

    public RoundRobinStage toEntity() {
        return RoundRobinStage.builder()
                .matchdayCount(matchdayCount)
                .splitSlotsPerRound(splitSlotsPerRound)
                .slotsPerRound(slotsPerRound)
                .build();
    }
}
