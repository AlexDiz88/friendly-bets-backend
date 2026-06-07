package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class MarathonbetSlotMatchPreviewDto {
    String gameResultId;
    int matchday;
    String homeTeamTitle;
    String awayTeamTitle;
    LocalDateTime utcDate;
    Long marathonbetTreeId;
    String marathonHomeTeam;
    String marathonAwayTeam;
    Long marathonDisplayTimeMillis;
    String matchStatus;
    boolean mappingOk;
    String mappingNote;
}
