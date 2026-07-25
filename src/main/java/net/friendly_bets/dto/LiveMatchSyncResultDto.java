package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.providers.LiveMatchProvider;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMatchSyncResultDto {
    private String leagueCode;
    private int updated;
    private int finishedDetected;
    private String message;

    public static LiveMatchSyncResultDto from(LiveMatchProvider.LiveSyncResult r) {
        if (r == null) {
            return LiveMatchSyncResultDto.builder().build();
        }
        return LiveMatchSyncResultDto.builder()
                .leagueCode(r.leagueCode())
                .updated(r.updated())
                .finishedDetected(r.finishedDetected())
                .message(r.message())
                .build();
    }
}
