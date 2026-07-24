package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultsToMatchSchedulesMigrationResultDto {

    private String seasonId;
    private String leagueCode;
    private String sourceSeasonYear;
    private int matchesRead;
    private int matchesUpserted;
    private int matchesSkipped;
    private int errors;
}
