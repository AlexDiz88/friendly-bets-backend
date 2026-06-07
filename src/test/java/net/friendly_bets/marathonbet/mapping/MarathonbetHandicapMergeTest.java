package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarathonbetHandicapMergeTest {

    private final MarathonbetBetTitleMapper mapper = new MarathonbetBetTitleMapper();

    @Test
    void threeDuplicateHbMarketsProduceOneHomeZeroRow() {
        MarathonbetMarketDto market = hbMarket("Канада (0)", "1.31", "Босния (0)", "3.10");
        MarathonbetExtractedMarkets markets = MarathonbetExtractedMarkets.builder()
                .handicapMarkets(List.of(market, market, market))
                .build();

        List<MappedOddsQuote> quotes = mapper.map(markets, "Канада", "Босния");
        long homeZeroQuotes = quotes.stream()
                .filter(q -> q.getBetTitle() != null
                        && q.getBetTitle().getCode() == BetTitleCode.HANDICAP_HOME_0.getCode())
                .count();
        assertEquals(1, homeZeroQuotes);

        OddsMergeResult merged = OddsMerger.merge(quotes);
        long homeZeroRows = merged.getMarketGroups().stream()
                .filter(g -> OddsMarketCategory.HANDICAP.name().equals(g.getCategory()))
                .flatMap(g -> g.getRows().stream())
                .filter(r -> r.getBetTitle() != null
                        && r.getBetTitle().getCode() == BetTitleCode.HANDICAP_HOME_0.getCode())
                .count();
        assertEquals(1, homeZeroRows);
    }

    private static MarathonbetMarketDto hbMarket(
            String homeSel,
            String homeOdds,
            String awaySel,
            String awayOdds
    ) {
        return MarathonbetMarketDto.builder()
                .model("MTCH_HB")
                .name("Победа с учетом форы")
                .selections(List.of(
                        selection(homeSel, homeOdds),
                        selection(awaySel, awayOdds)
                ))
                .build();
    }

    private static MarathonbetMarketSelectionDto selection(String name, String odds) {
        return MarathonbetMarketSelectionDto.builder()
                .name(name)
                .odds(new BigDecimal(odds))
                .build();
    }
}
