package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.external.ExternalMatchdaySync;
import net.friendly_bets.models.external.ExternalMatchdaySyncStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchdaySyncDto {

    private String competitionCode;
    private int matchday;
    private String season;
    private ExternalMatchdaySyncStatus syncStatus;
    private int expectedMatchCount;
    private int finishedMatchCount;
    private LocalDateTime lastFetchedAt;
    private LocalDateTime completedAt;

    public static ExternalMatchdaySyncDto from(ExternalMatchdaySync sync) {
        return ExternalMatchdaySyncDto.builder()
                .competitionCode(sync.getCompetitionCode())
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
