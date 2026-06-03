package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class MarathonbetMarketExtractor {

    private MarathonbetMarketExtractor() {
    }

    public static MarathonbetExtractedMarkets extract(JsonNode root, Set<String> models) {
        JsonNode marketsNode = root != null ? root.get("markets") : null;
        if (marketsNode == null || !marketsNode.isObject()) {
            return MarathonbetExtractedMarkets.builder()
                    .matchResultMarkets(List.of())
                    .handicapMarkets(List.of())
                    .totalMarkets(List.of())
                    .correctScoreMarkets(List.of())
                    .doubleChanceMarkets(List.of())
                    .build();
        }

        List<MarathonbetMarketDto> matchResult = new ArrayList<>();
        List<MarathonbetMarketDto> handicaps = new ArrayList<>();
        List<MarathonbetMarketDto> totals = new ArrayList<>();
        List<MarathonbetMarketDto> correctScore = new ArrayList<>();
        List<MarathonbetMarketDto> doubleChance = new ArrayList<>();

        Iterator<String> fieldNames = marketsNode.fieldNames();
        while (fieldNames.hasNext()) {
            JsonNode market = marketsNode.get(fieldNames.next());
            if (market == null || market.isNull()) {
                continue;
            }
            String model = text(market.get("model"));
            if (model == null || (models != null && !models.isEmpty() && !models.contains(model))) {
                continue;
            }
            MarathonbetMarketDto dto = toMarketDto(market);
            switch (model) {
                case "MTCH_R", "MTCH_R1", "MTCH_R2" -> matchResult.add(dto);
                case "MTCH_HB", "MTCH_HB1", "MTCH_HB2" -> handicaps.add(dto);
                case "MTCH_TTLG", "MTCH_TTLG1", "MTCH_TTLG2" -> totals.add(dto);
                case "MTCH_DC", "MTCH_DC1", "MTCH_DC2" -> doubleChance.add(dto);
                case "MTCH_CSDYN", "MTCH_CSW1DYN", "MTCH_CSW2DYN" -> correctScore.add(dto);
                default -> {
                    if (model != null && model.startsWith("MTCH_CS")) {
                        correctScore.add(dto);
                    }
                }
            }
        }

        handicaps.sort(handicapComparator());
        totals.sort(totalComparator());
        correctScore.sort(correctScoreComparator());

        return MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(matchResult)
                .handicapMarkets(handicaps)
                .totalMarkets(totals)
                .correctScoreMarkets(correctScore)
                .doubleChanceMarkets(doubleChance)
                .build();
    }

    public static MarathonbetExtractedMarkets extractAll(JsonNode root) {
        return extract(root, null);
    }

    private static Comparator<MarathonbetMarketDto> handicapComparator() {
        return Comparator.comparing(MarathonbetMarketExtractor::handicapSortKey);
    }

    private static Comparator<MarathonbetMarketDto> totalComparator() {
        return Comparator.comparing(MarathonbetMarketExtractor::totalSortKey);
    }

    private static Comparator<MarathonbetMarketDto> correctScoreComparator() {
        return Comparator.comparing(MarathonbetMarketExtractor::correctScoreSortKey);
    }

    private static String handicapSortKey(MarathonbetMarketDto market) {
        double min = Double.MAX_VALUE;
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            Double line = parseHandicapLine(sel.getName());
            if (line != null) {
                min = Math.min(min, line);
            }
        }
        return String.format("%015.3f", min == Double.MAX_VALUE ? 9999 : min);
    }

    private static String totalSortKey(MarathonbetMarketDto market) {
        double min = Double.MAX_VALUE;
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            Double line = parseTotalLine(sel.getName());
            if (line != null) {
                min = Math.min(min, line);
            }
        }
        return String.format("%015.3f", min == Double.MAX_VALUE ? 9999 : min);
    }

    private static String correctScoreSortKey(MarathonbetMarketDto market) {
        int min = Integer.MAX_VALUE;
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            int key = MarathonbetSelectionParsing.correctScoreSortKey(sel.getName());
            if (key < min) {
                min = key;
            }
        }
        return String.format("%010d", min == Integer.MAX_VALUE ? 999999999 : min);
    }

    static Double parseHandicapLine(String selectionName) {
        return MarathonbetSelectionParsing.parseHandicapLine(selectionName);
    }

    static Double parseTotalLine(String selectionName) {
        return MarathonbetSelectionParsing.parseTotalLine(selectionName);
    }

    private static MarathonbetMarketDto toMarketDto(JsonNode market) {
        List<MarathonbetMarketSelectionDto> selections = new ArrayList<>();
        JsonNode selectionsNode = market.get("selections");
        if (selectionsNode != null && selectionsNode.isObject()) {
            Iterator<String> ids = selectionsNode.fieldNames();
            while (ids.hasNext()) {
                JsonNode sel = selectionsNode.get(ids.next());
                if (sel == null || sel.isNull()) {
                    continue;
                }
                selections.add(MarathonbetMarketSelectionDto.builder()
                        .selId(sel.hasNonNull("selId") ? sel.get("selId").asLong() : null)
                        .name(text(sel.get("name")))
                        .odds(decimalOdds(sel.get("coeff")))
                        .build());
            }
        }
        selections.sort(Comparator.comparing(
                s -> s.getName() != null ? s.getName() : "",
                String.CASE_INSENSITIVE_ORDER
        ));
        String name = text(market.get("name"));
        return MarathonbetMarketDto.builder()
                .marketId(market.hasNonNull("marketId") ? market.get("marketId").asLong() : null)
                .model(text(market.get("model")))
                .name(name)
                .selections(selections)
                .ignoredForProd(MarathonbetProdMarketFilter.isIgnoredForProd(name))
                .build();
    }

    static BigDecimal decimalOdds(JsonNode coeffNode) {
        if (coeffNode == null || coeffNode.isNull()) {
            return null;
        }
        JsonNode price = coeffNode.get("price");
        if (price == null || !price.has("n") || !price.has("d")) {
            return null;
        }
        int n = price.get("n").asInt();
        int d = price.get("d").asInt();
        if (d == 0) {
            return null;
        }
        return BigDecimal.valueOf(1.0 + (double) n / d).setScale(3, RoundingMode.HALF_UP);
    }

    static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    public static String memberName(JsonNode root, String teamField) {
        JsonNode team = root.get(teamField);
        if (team == null) {
            return null;
        }
        JsonNode members = team.get("members");
        if (members == null || !members.isArray() || members.isEmpty()) {
            return null;
        }
        return text(members.get(0).get("name"));
    }
}
