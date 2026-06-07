package net.friendly_bets.marathonbet;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.dto.MarathonbetMarketDto;

import java.util.List;

@Value
@Builder
public class MarathonbetExtractedMarkets {
    @Builder.Default
    List<MarathonbetMarketDto> matchResultMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> halfTimeResultMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> secondHalfResultMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> handicapMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> halfTimeHandicapMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> secondHalfHandicapMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> totalMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> halfTimeTotalMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> secondHalfTotalMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> teamTotalHomeMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> teamTotalAwayMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> correctScoreMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> firstHalfCorrectScoreMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> secondHalfCorrectScoreMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> doubleChanceMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> halfTimeDoubleChanceMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> secondHalfDoubleChanceMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> resultTotalMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> goalsMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> cleanWinMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> scoreDiffMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> halfFullMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> firstSecondHalfMarkets = List.of();
    @Builder.Default
    List<MarathonbetMarketDto> bttsResultMarkets = List.of();
}
