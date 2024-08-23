package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameResultDto {

    private String fullTime;
    private String firstTime;
    private String overTime;
    private String penalty;

    public static GameResultDto from(GameResult gameResult) {
        return GameResultDto.builder()
                .fullTime(gameResult.getFullTime())
                .firstTime(gameResult.getFirstTime())
                .overTime(gameResult.getOverTime())
                .penalty(gameResult.getPenalty())
                .build();
    }
}
