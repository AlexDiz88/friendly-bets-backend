package net.friendly_bets.models.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "external_sync_issues")
public class ExternalSyncIssue {

    public enum Provider {
        FOOTBALL_DATA
    }

    public enum IssueType {
        TEAM_MAPPING_MISSING
    }

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    @Indexed
    private LocalDateTime createdAt;

    @Field(name = "provider")
    private String provider;

    @Field(name = "issue_type")
    private String issueType;

    @Field(name = "competition_code")
    private String competitionCode;

    @Field(name = "season")
    private String season;

    @Field(name = "matchday")
    private Integer matchday;

    @Field(name = "external_match_id")
    private Long externalMatchId;

    @Field(name = "home_team_name")
    private String homeTeamName;

    @Field(name = "away_team_name")
    private String awayTeamName;

    @Field(name = "home_team_external_id")
    private Integer homeTeamExternalId;

    @Field(name = "away_team_external_id")
    private Integer awayTeamExternalId;

    @Field(name = "message")
    private String message;
}

