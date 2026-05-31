package net.friendly_bets.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class PatchMatchResultSyncSettingsDto {

    private String primaryProvider;
    private String secondaryProvider;
    private Boolean dualVerificationEnabled;
    private Boolean allowFinalizeWithoutSecondary;

    @Min(1)
    @Max(10)
    private Integer requireStablePolls;

    @Min(90)
    @Max(300)
    private Integer minMinutesAfterKickoff;

    @Min(90)
    @Max(360)
    private Integer minMinutesAfterKickoffKnockout;

    @Min(0)
    @Max(180)
    private Integer minMinutesSinceApiLastUpdated;

    private Boolean autoSettleOnlyWhenMatchdayCompleted;
}
