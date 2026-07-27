package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataSandboxScheduleRequestDto {
    private String provider;
    /** soccer365 competition id */
    private Integer competitionId;
    /** sports.ru tournament slug ({@code premier-league}) or full calendar path */
    private String calendarPath;
    /** Optional: keep only this round number. */
    private Integer round;
    /** Optional: max matches in parsed response (after round filter). */
    private Integer limit;
}
