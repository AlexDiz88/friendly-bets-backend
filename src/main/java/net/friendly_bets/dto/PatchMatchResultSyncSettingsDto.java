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

    private Boolean autoSettleOnlyWhenMatchdayCompleted;
}
