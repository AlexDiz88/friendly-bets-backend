package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UnmappedExternalTeamNameDto {

    private String externalName;
    private Integer externalId;
    /** Provider id, e.g. soccer365.ru or marathonbet */
    private String provider;
}
