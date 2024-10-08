package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameResult;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BetResult {

    @NotNull(message = "{field.isNull}")
    private GameResult gameResult;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankBetStatus}")
    private String betStatus;
}
