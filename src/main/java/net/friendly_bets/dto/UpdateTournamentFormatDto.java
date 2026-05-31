package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTournamentFormatDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String name;

    @Valid
    private RoundRobinStageDto regularStage;

    @Valid
    private RoundRobinStageDto groupStage;

    @Valid
    private List<PlayoffRoundDto> playoff;
}
