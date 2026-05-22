package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.RoundRobinStage;

import javax.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundRobinStageDto {

    @Min(1)
    private int matchdayCount;

    public static RoundRobinStageDto from(RoundRobinStage stage) {
        if (stage == null) {
            return null;
        }
        return RoundRobinStageDto.builder()
                .matchdayCount(stage.getMatchdayCount())
                .build();
    }

    public RoundRobinStage toEntity() {
        return RoundRobinStage.builder()
                .matchdayCount(matchdayCount)
                .build();
    }
}
