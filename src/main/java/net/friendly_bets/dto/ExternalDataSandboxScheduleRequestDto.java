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
    /** soccer365 competition id; football24.ua internal league id (EPL=3, BL=7, CL=1, LE=2) */
    private Integer competitionId;
    /** sports.ru tournament slug ({@code premier-league}) or full calendar path */
    private String calendarPath;
    /** Optional: keep only this round number. */
    private Integer round;
    /** Optional: max matches in parsed response (after round filter). */
    private Integer limit;
}
