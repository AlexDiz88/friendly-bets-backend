package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wc26GameResultLookupDto {

    private String gameResultId;
    private int wc26ScheduleId;
    private String homeTeamId;
    private String awayTeamId;
    private LocalDateTime kickoffUtc;
    private String slotId;
}
