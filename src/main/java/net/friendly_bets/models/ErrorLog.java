package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "error_logs")
@CompoundIndexes({
        @CompoundIndex(name = "created_at_desc", def = "{'created_at': -1}"),
        @CompoundIndex(name = "layer_created", def = "{'layer': 1, 'created_at': -1}"),
        @CompoundIndex(name = "provider_code_match", def = "{'provider': 1, 'code': 1, 'match_schedule_id': 1}")
})
public class ErrorLog {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    @Indexed
    private Instant createdAt;

    /** ERROR or WARN */
    @Field(name = "severity")
    private String severity;

    /** SCHEDULE / ODDS / LIVE / FULL_MATCH — optional for non-layer errors */
    @Field(name = "layer")
    private String layer;

    @Field(name = "provider")
    private String provider;

    /** PRIMARY / SECONDARY */
    @Field(name = "provider_role")
    private String providerRole;

    /** Stable machine code, e.g. teamMappingMissing, soccer365FetchFailed */
    @Field(name = "code")
    private String code;

    @Field(name = "message")
    private String message;

    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "season")
    private String season;

    @Field(name = "matchday")
    private Integer matchday;

    @Field(name = "match_schedule_id")
    private String matchScheduleId;

    @Field(name = "external_match_id")
    private String externalMatchId;

    @Field(name = "home_team")
    private String homeTeam;

    @Field(name = "away_team")
    private String awayTeam;

    /** Extra chips: homeExternalId, awayExternalId, … */
    @Field(name = "context")
    @Builder.Default
    private Map<String, String> context = new LinkedHashMap<>();
}
