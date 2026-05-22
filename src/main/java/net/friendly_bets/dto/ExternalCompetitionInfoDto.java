package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCompetitionInfoDto {

    private String competitionCode;
    private String season;
    private String leagueId;
    private int currentMatchday;
    private int matchdayCount;
    private List<ExternalMatchdaySlotDto> matchdaySlots;
}
