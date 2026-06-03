package net.friendly_bets.marathonbet;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.dto.MarathonbetMarketDto;

import java.util.List;

@Value
@Builder
public class MarathonbetExtractedMarkets {
    List<MarathonbetMarketDto> matchResultMarkets;
    List<MarathonbetMarketDto> handicapMarkets;
    List<MarathonbetMarketDto> totalMarkets;
    List<MarathonbetMarketDto> correctScoreMarkets;
    List<MarathonbetMarketDto> doubleChanceMarkets;
}
