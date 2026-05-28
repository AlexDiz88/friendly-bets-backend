package net.friendly_bets.oddsapi;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ParsedOddsMarket {

    String name;
    String updatedAt;
    List<ParsedOddsLine> lines;

    @Value
    @Builder
    public static class ParsedOddsLine {
        String line;
        /** selection key → decimal odds string */
        Map<String, String> prices;
    }
}
