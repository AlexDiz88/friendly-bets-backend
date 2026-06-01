package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class OddsDemoDebugDto {

    long oddsApiEventId;
    String home;
    String away;
    List<String> bookmakers;
    LocalDateTime fetchedAt;
    Map<String, List<Object>> rawBookmakers;
    Map<String, List<OddsMappingTraceEntryDto>> mappingTraceByBookmaker;
    List<OddsMarketGroup> mergedMarketGroups;
    List<String> mappingIssues;
}
