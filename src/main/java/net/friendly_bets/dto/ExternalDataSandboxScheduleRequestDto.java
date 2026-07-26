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
    /** aiscore league code → tournament path from config (EPL/BL/CL/LE) */
    private String leagueCode;
    /** aiscore tournament path override, e.g. tournament-english-premier-league/mo07dni2vfxknxy */
    private String tournamentPath;
    /** Optional: keep only this round number. */
    private Integer round;
    /** Optional: max matches in parsed response (after round filter). */
    private Integer limit;
}
