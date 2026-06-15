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
    /** {@link net.friendly_bets.gameresults.MatchDataProviders} id, e.g. 4score.ru or odds-api.io */
    private String provider;
}
