package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewTournamentFormatDto {

    @NotBlank
    private String formatCode;

    @NotBlank
    private String name;

    @Valid
    private RoundRobinStageDto regularStage;

    @Valid
    private RoundRobinStageDto groupStage;

    @Valid
    private List<PlayoffRoundDto> playoff;
}
