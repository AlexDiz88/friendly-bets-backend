package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class MarathonbetScrapeResultDto {
    long treeId;
    Long eventId;
    String eventName;
    String competitionHeader;
    String homeTeam;
    String awayTeam;
    Instant startTime;
    String sourceUrl;
    Instant fetchedAt;
    List<MarathonbetMarketDto> matchResultMarkets;
    List<MarathonbetMarketDto> handicapMarkets;
    List<String> warnings;
}
