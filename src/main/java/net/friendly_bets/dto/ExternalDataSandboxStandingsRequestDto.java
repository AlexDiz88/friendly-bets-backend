package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataSandboxStandingsRequestDto {
    private String provider;
    /** League code, e.g. EPL or BL. */
    private String leagueCode;
}
