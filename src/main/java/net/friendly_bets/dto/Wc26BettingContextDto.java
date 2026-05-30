package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wc26BettingContextDto {

    private boolean bettingEnabled;
    private String seasonId;
    private String leagueId;
    private String leagueCode;
    private String tournamentFormatId;
    private boolean seasonParticipant;
}
