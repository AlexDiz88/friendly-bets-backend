package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.providers.LiveMatchProvider;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMatchSyncResultDto {
    private int httpRequests;
    private int trackedCount;
    private int updated;
    private int finishedDetected;
    private String message;
    private List<String> datesSynced;

    public static LiveMatchSyncResultDto from(LiveMatchProvider.LiveSyncResult r) {
        if (r == null) {
            return LiveMatchSyncResultDto.builder().build();
        }
        return LiveMatchSyncResultDto.builder()
                .httpRequests(r.httpRequests())
                .trackedCount(r.trackedCount())
                .updated(r.updated())
                .finishedDetected(r.finishedDetected())
                .message(r.message())
                .datesSynced(r.datesSynced())
                .build();
    }
}
