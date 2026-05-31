package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.PlayoffRound;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayoffRoundDto {

    @NotBlank
    private String stage;

    @Min(1)
    @Max(8)
    private int matchdayCount;

    public static PlayoffRoundDto from(PlayoffRound round) {
        if (round == null) {
            return null;
        }
        return PlayoffRoundDto.builder()
                .stage(round.getStage())
                .matchdayCount(round.getMatchdayCount())
                .build();
    }

    public PlayoffRound toEntity() {
        return PlayoffRound.builder()
                .stage(stage)
                .matchdayCount(matchdayCount)
                .build();
    }
}
