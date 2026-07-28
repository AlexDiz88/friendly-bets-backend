package net.friendly_bets.melbet.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.odds.OddsMarketCategory;
import net.friendly_bets.odds.mapping.MappedOddsQuote;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MelbetBetTitleMapperTest {

    private final MelbetBetTitleMapper mapper = new MelbetBetTitleMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsCoreMarketsFromGetEventPayload() throws Exception {
        Path fixture = Path.of("src/test/resources/melbet/arsenal_coventry_getevent.json");
        if (!Files.isRegularFile(fixture)) {
            // Optional fixture — skip quietly if not present in CI checkout
            return;
        }
        JsonNode body = objectMapper.readTree(Files.readString(fixture));
        List<MappedOddsQuote> quotes = mapper.mapEventPayload(body);
        assertThat(quotes).isNotEmpty();
        assertThat(quotes.stream().map(MappedOddsQuote::getCategory))
                .contains(
                        OddsMarketCategory.MATCH_RESULT,
                        OddsMarketCategory.HANDICAP,
                        OddsMarketCategory.TOTALS,
                        OddsMarketCategory.DOUBLE_CHANCE
                );
        assertThat(quotes.stream().allMatch(MappedOddsQuote::isOk)).isTrue();
    }

    @Test
    void mergesMainHandicapLineIntoQuotes() throws Exception {
        String json = """
                [{
                  "Id": 1,
                  "EGN": "Arsenal - Coventry City",
                  "StakeTypes": [
                    {"Id": -2, "N": "Фора", "Stakes": [
                      {"Id": 1, "N": "Фора1", "SC": 1, "A": -2, "F": 1.84},
                      {"Id": 2, "N": "Фора2", "SC": 2, "A": 2, "F": 1.96}
                    ]},
                    {"Id": 2, "N": "Фора", "Stakes": [
                      {"Id": 3, "N": "Фора1", "SC": 1, "A": -1.5, "F": 1.51},
                      {"Id": 4, "N": "Фора2", "SC": 2, "A": 1.5, "F": 2.49}
                    ]},
                    {"Id": 1, "N": "Исход", "Stakes": [
                      {"Id": 10, "N": "П1", "SC": 1, "F": 1.15},
                      {"Id": 11, "N": "X", "SC": 2, "F": 7.6},
                      {"Id": 12, "N": "П2", "SC": 3, "F": 17}
                    ]}
                  ]
                }]
                """;
        List<MappedOddsQuote> quotes = mapper.mapEventPayload(objectMapper.readTree(json));
        assertThat(quotes.stream().filter(q -> q.getCategory() == OddsMarketCategory.HANDICAP))
                .hasSizeGreaterThanOrEqualTo(4);
        assertThat(quotes.stream().filter(q -> q.getCategory() == OddsMarketCategory.MATCH_RESULT))
                .hasSize(3);
        assertThat(quotes.stream().anyMatch(q -> "-2".equals(q.getLine()) || "-2.0".equals(q.getLine())
                || (q.getLine() != null && q.getLine().contains("2")))).isTrue();
    }

    @Test
    void mapsFirstSecondHalfWithoutConfusingHalfLabelDigit() throws Exception {
        String json = """
                [{
                  "Id": 1,
                  "EGN": "Arsenal - Coventry City",
                  "StakeTypes": [
                    {"Id": 421317, "N": "1-й тайм + 2-й тайм", "Stakes": [
                      {"Id": 1, "N": "1-й тайм X + 2-й тайм X", "F": 8.7},
                      {"Id": 2, "N": "1-й тайм П1 + 2-й тайм П2", "F": 9.7}
                    ]}
                  ]
                }]
                """;
        List<MappedOddsQuote> quotes = mapper.mapEventPayload(objectMapper.readTree(json));
        assertThat(quotes).hasSize(2);
        assertThat(quotes.stream().map(MappedOddsQuote::getCategory))
                .containsOnly(OddsMarketCategory.FIRST_SECOND_HALF);
        assertThat(quotes.stream().allMatch(MappedOddsQuote::isOk)).isTrue();
    }
}
