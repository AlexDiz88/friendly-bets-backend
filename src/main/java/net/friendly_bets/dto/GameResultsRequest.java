package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameResult;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameResultsRequest {

    @NotNull(message = "{field.userId.isNull}")
    @NotBlank(message = "{field.userId.isBlank}")
    private String seasonId;

    @NotNull(message = "{field.isNull}")
    @NotEmpty(message = "{field.isEmpty}")
    private List<GameResult> gameResults;
}
