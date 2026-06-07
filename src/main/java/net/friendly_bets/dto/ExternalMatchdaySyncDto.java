package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchdaySyncDto {

    private String leagueCode;
    private int matchday;
    private String season;
    private GameResultsSyncStatus syncStatus;
    private int expectedMatchCount;
    private int finishedMatchCount;
    private LocalDateTime lastFetchedAt;
    private LocalDateTime completedAt;

    public static ExternalMatchdaySyncDto from(GameResultsSync sync) {
        return ExternalMatchdaySyncDto.builder()
                .leagueCode(sync.getLeagueCode())
                .matchday(sync.getMatchday())
                .season(sync.getSeason())
                .syncStatus(sync.getSyncStatus())
                .expectedMatchCount(sync.getExpectedMatchCount())
                .finishedMatchCount(sync.getFinishedMatchCount())
                .lastFetchedAt(sync.getLastFetchedAt())
                .completedAt(sync.getCompletedAt())
                .build();
    }
}
