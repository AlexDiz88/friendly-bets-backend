package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataSandboxFullMatchRequestDto {
    private String provider;
    /** Direct game/event id (soccer365 game id or ruscore eventId / slug/eventId). */
    private String gameId;
    /** Optional day browse (ruscore): ISO date yyyy-MM-dd. */
    private String date;
    /** Optional competition title filter for day browse. */
    private String titleContains;
}
