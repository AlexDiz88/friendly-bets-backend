package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetBetTitleMapperHandicapTest {

    private final MarathonbetBetTitleMapper mapper = new MarathonbetBetTitleMapper();

    @Test
    void preservesAwayPlusSignFromMarathonSelectionName() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_HB")
                .name("Победа с учетом форы")
                .selections(List.of(
                        selection("Мексика (-1)", "1.70"),
                        selection("ЮАР (+1)", "2.16")
                ))
                .build();

        List<MappedOddsQuote> quotes = mapper.map(
                MarathonbetExtractedMarkets.builder().handicapMarkets(List.of(market)).build(),
                "Мексика",
                "ЮАР"
        );

        MappedOddsQuote home = findQuote(quotes, BetTitleCode.HANDICAP_HOME_MINUS_1_0);
        MappedOddsQuote away = findQuote(quotes, BetTitleCode.HANDICAP_AWAY_PLUS_1_0);

        assertEquals("HOME", home.getSelectionCode());
        assertEquals("-1", home.getLine());
        assertEquals("AWAY", away.getSelectionCode());
        assertEquals("1", away.getLine());
    }

    @Test
    void preservesAwayPlusOneAndAHalf() {
        MarathonbetMarketDto market = MarathonbetMarketDto.builder()
                .model("MTCH_HB")
                .name("Победа с учетом форы")
                .selections(List.of(
                        selection("Мексика (-1.5)", "2.21"),
                        selection("ЮАР (+1.5)", "1.59")
                ))
                .build();

        List<MappedOddsQuote> quotes = mapper.map(
                MarathonbetExtractedMarkets.builder().handicapMarkets(List.of(market)).build(),
                "Мексика",
                "ЮАР"
        );

        assertTrue(quotes.stream().anyMatch(q -> q.getBetTitle().getCode() == BetTitleCode.HANDICAP_AWAY_PLUS_1_5.getCode()));
        MappedOddsQuote away = findQuote(quotes, BetTitleCode.HANDICAP_AWAY_PLUS_1_5);
        assertEquals("1.5", away.getLine());
    }

    private static MarathonbetMarketSelectionDto selection(String name, String odds) {
        return MarathonbetMarketSelectionDto.builder()
                .name(name)
                .odds(new BigDecimal(odds))
                .build();
    }

    private static MappedOddsQuote findQuote(List<MappedOddsQuote> quotes, BetTitleCode code) {
        return quotes.stream()
                .filter(q -> q.getCategory() == OddsMarketCategory.HANDICAP
                        && q.getBetTitle() != null
                        && q.getBetTitle().getCode() == code.getCode())
                .findFirst()
                .orElseThrow();
    }
}
