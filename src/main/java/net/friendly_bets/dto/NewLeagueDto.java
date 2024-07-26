package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewLeagueDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String leagueCode;
}
