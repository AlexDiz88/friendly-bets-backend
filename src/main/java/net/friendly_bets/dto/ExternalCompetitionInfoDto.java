package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCompetitionInfoDto {

    private String competitionCode;
    private String season;
    private int currentMatchday;
    private int matchdayCount;
}
