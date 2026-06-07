package net.friendly_bets.models.gameresults;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

/** Singleton runtime-override настроек синхронизации результатов (админка). */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "match_result_sync_settings")
public class MatchResultSyncSettings {

    public static final String SINGLETON_ID = "default";

    @MongoId
    @Field(name = "_id")
    @Builder.Default
    private String id = SINGLETON_ID;

    @Field(name = "primary_provider")
    private String primaryProvider;

    @Field(name = "secondary_provider")
    private String secondaryProvider;

    @Field(name = "dual_verification_enabled")
    private Boolean dualVerificationEnabled;

    @Field(name = "allow_finalize_without_secondary")
    private Boolean allowFinalizeWithoutSecondary;

    @Field(name = "require_stable_polls")
    private Integer requireStablePolls;

    @Field(name = "min_minutes_after_kickoff")
    private Integer minMinutesAfterKickoff;

    @Field(name = "min_minutes_after_kickoff_knockout")
    private Integer minMinutesAfterKickoffKnockout;

    @Field(name = "min_minutes_since_api_last_updated")
    private Integer minMinutesSinceApiLastUpdated;

    @Field(name = "auto_settle_only_when_matchday_completed")
    private Boolean autoSettleOnlyWhenMatchdayCompleted;

    @Field(name = "updated_at")
    private LocalDateTime updatedAt;
}
