package net.friendly_bets.models.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Layer-specific counters; unused fields stay null/0.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExternalApiMonitoringCounters {

    @Field(name = "requested")
    private Integer requested;

    @Field(name = "upserted")
    private Integer upserted;

    @Field(name = "updated")
    private Integer updated;

    @Field(name = "matched")
    private Integer matched;

    @Field(name = "saved")
    private Integer saved;

    @Field(name = "skipped")
    private Integer skipped;

    @Field(name = "mapping_failures")
    private Integer mappingFailures;

    @Field(name = "sse_calls")
    private Integer sseCalls;

    @Field(name = "rounds_parsed")
    private Integer roundsParsed;

    @Field(name = "finished_detected")
    private Integer finishedDetected;

    @Field(name = "tournament_fetched")
    private Boolean tournamentFetched;

    @Field(name = "eligible")
    private Integer eligible;
}
