package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.OddsMarket;
import net.friendly_bets.models.odds.OddsOutcome;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsStoredMarketsConverterTest {

    @Test
    void roundtripIntegerHandicapsRemainBettable() {
        OddsMarket spread = OddsMarket.builder()
                .name("Spread")
                .outcomes(List.of(
                        OddsOutcome.builder().label("home").odds("7.900").line("-3.0").build(),
                        OddsOutcome.builder().label("away").odds("1.022").line("-3.0").build(),
                        OddsOutcome.builder().label("home").odds("3.420").line("-2.0").build(),
                        OddsOutcome.builder().label("away").odds("1.250").line("-2.0").build(),
                        OddsOutcome.builder().label("home").odds("2.250").line("-1.5").build(),
                        OddsOutcome.builder().label("away").odds("1.570").line("-1.5").build()
                ))
                .build();

        List<OddsApiMarketDto> apiMarkets = OddsStoredMarketsConverter.toApiMarkets(List.of(spread));
        Map<String, List<OddsApiMarketDto>> byBk = Map.of("1xbet", apiMarkets);

        List<net.friendly_bets.models.odds.OddsMarketGroup> groups = OddsGroupBuilder.build(
                byBk,
                OddsBookmakerKeys.mapApiKeysToConfigured(List.of("1xbet")),
                OddsMatchContext.of("Mexico", "South Africa")
        );

        var handicap = groups.stream()
                .filter(g -> "HANDICAP".equals(g.getCategory()))
                .findFirst()
                .orElseThrow();

        assertTrue(handicap.getRows().size() >= 6);
        assertTrue(handicap.getRows().stream().anyMatch(r ->
                "HOME".equals(r.getSelectionCode()) && "-3".equals(r.getLine())));
        assertTrue(handicap.getRows().stream().anyMatch(r ->
                "HOME".equals(r.getSelectionCode()) && "-2".equals(r.getLine())));

        for (var row : handicap.getRows()) {
            assertDoesNotThrow(() -> OddsSelectionBetTitleMapper.toBetTitle("HANDICAP", row));
        }

        long minusThreeHome = handicap.getRows().stream()
                .filter(r -> "HOME".equals(r.getSelectionCode()) && "-3".equals(r.getLine()))
                .count();
        assertEquals(1, minusThreeHome);
    }
}
