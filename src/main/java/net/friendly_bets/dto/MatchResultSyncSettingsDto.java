package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultSyncSettingsDto {

    private String primaryProvider;
    private String secondaryProvider;
    private boolean dualVerificationEnabled;
    private int requireStablePolls;
    private int minMinutesAfterKickoff;
    private boolean autoSettleEnabled;
}
