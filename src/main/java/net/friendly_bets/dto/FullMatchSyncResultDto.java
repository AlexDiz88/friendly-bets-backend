package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullMatchSyncResultDto {
    private String provider;
    private String date;
    private int candidates;
    private int succeeded;
    private int notReady;
    private int failed;
}
