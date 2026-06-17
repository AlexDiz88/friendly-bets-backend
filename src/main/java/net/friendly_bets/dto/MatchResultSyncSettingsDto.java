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
    private boolean allowFinalizeWithoutSecondary;
    private int requireStablePolls;
    private int minMinutesAfterKickoff;
    private boolean autoSettleEnabled;
    private boolean autoSettleOnlyWhenMatchdayCompleted;

    /** Значения из application.properties (read-only для UI). */
    private MatchResultSyncSettingsDto envDefaults;
}
