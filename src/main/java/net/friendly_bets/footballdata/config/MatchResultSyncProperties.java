package net.friendly_bets.footballdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "match-result-sync")
public class MatchResultSyncProperties {

    private String primaryProvider = "football-data";
    private String secondaryProvider = "api-football";
    private boolean dualVerificationEnabled = false;
    private boolean allowFinalizeWithoutSecondary = false;
    private int requireStablePolls = 2;
    /** Минут после utcDate kickoff для обычного матча. */
    private int minMinutesAfterKickoff = 105;
    /** Минут после utcDate для плей-офф (ОТ/пен). */
    private int minMinutesAfterKickoffKnockout = 150;
    private int minMinutesSinceApiLastUpdated = 30;
    private boolean autoSettleOnlyWhenMatchdayCompleted = false;
}
