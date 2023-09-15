package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveSeasonIdDto {

    @Schema(description = "ID текущего (активного) сезона", example = "64d5559adcbdb83bc5d60638")
    private String value;
}
