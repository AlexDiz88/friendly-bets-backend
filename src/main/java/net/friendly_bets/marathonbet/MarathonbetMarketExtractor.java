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

    /**
     * @deprecated Используйте {@link #extractAll(JsonNode)} — tournament JSON больше не несёт prod-кэфы.
     */
    @Deprecated
    public static MarathonbetExtractedMarkets extract(JsonNode root, Set<String> models) {
        if (models != null && !models.isEmpty()) {
            return extractFiltered(root, models);
        }
        return extractAll(root);
    }

    public static MarathonbetExtractedMarkets extractAll(JsonNode root) {
        JsonNode marketsNode = root != null ? root.get("markets") : null;
        if (marketsNode == null || !marketsNode.isObject()) {
            return empty();
        }

        List<MarathonbetMarketDto> matchResult = new ArrayList<>();
        List<MarathonbetMarketDto> halfTimeResult = new ArrayList<>();
        List<MarathonbetMarketDto> secondHalfResult = new ArrayList<>();
        List<MarathonbetMarketDto> handicaps = new ArrayList<>();
        List<MarathonbetMarketDto> halfTimeHandicaps = new ArrayList<>();
        List<MarathonbetMarketDto> secondHalfHandicaps = new ArrayList<>();
        List<MarathonbetMarketDto> totals = new ArrayList<>();
        List<MarathonbetMarketDto> halfTimeTotals = new ArrayList<>();
        List<MarathonbetMarketDto> secondHalfTotals = new ArrayList<>();
        List<MarathonbetMarketDto> teamTotalHome = new ArrayList<>();
        List<MarathonbetMarketDto> teamTotalAway = new ArrayList<>();
        List<MarathonbetMarketDto> correctScore = new ArrayList<>();
        List<MarathonbetMarketDto> firstHalfCorrectScore = new ArrayList<>();
        List<MarathonbetMarketDto> secondHalfCorrectScore = new ArrayList<>();
        List<MarathonbetMarketDto> doubleChance = new ArrayList<>();
        List<MarathonbetMarketDto> halfTimeDoubleChance = new ArrayList<>();
        List<MarathonbetMarketDto> secondHalfDoubleChance = new ArrayList<>();
        List<MarathonbetMarketDto> resultTotal = new ArrayList<>();
        List<MarathonbetMarketDto> goals = new ArrayList<>();
        List<MarathonbetMarketDto> cleanWin = new ArrayList<>();
        List<MarathonbetMarketDto> scoreDiff = new ArrayList<>();
        List<MarathonbetMarketDto> halfFull = new ArrayList<>();
        List<MarathonbetMarketDto> firstSecondHalf = new ArrayList<>();
        List<MarathonbetMarketDto> bttsResult = new ArrayList<>();

        Iterator<String> fieldNames = marketsNode.fieldNames();
        while (fieldNames.hasNext()) {
            JsonNode market = marketsNode.get(fieldNames.next());
            if (market == null || market.isNull()) {
                continue;
            }
            String model = text(market.get("model"));
            if (!MarathonbetAllowedMarkets.isAllowedModel(model)) {
                continue;
            }
            MarathonbetMarketDto dto = toMarketDto(market);
            if (MarathonbetProdMarketFilter.isIgnoredForProd(dto)) {
                continue;
            }
            MarathonbetMarketBucket bucket = MarathonbetAllowedMarkets.bucketFor(model).orElseThrow();
            switch (bucket) {
                case MATCH_RESULT -> matchResult.add(dto);
                case HALF_TIME_RESULT -> halfTimeResult.add(dto);
                case SECOND_HALF_RESULT -> secondHalfResult.add(dto);
                case DOUBLE_CHANCE -> doubleChance.add(dto);
                case HALF_TIME_DOUBLE_CHANCE -> halfTimeDoubleChance.add(dto);
                case SECOND_HALF_DOUBLE_CHANCE -> secondHalfDoubleChance.add(dto);
                case HANDICAP -> handicaps.add(dto);
                case HALF_TIME_HANDICAP -> halfTimeHandicaps.add(dto);
                case SECOND_HALF_HANDICAP -> secondHalfHandicaps.add(dto);
                case TOTALS -> totals.add(dto);
                case HALF_TIME_TOTALS -> halfTimeTotals.add(dto);
                case SECOND_HALF_TOTALS -> secondHalfTotals.add(dto);
                case TEAM_TOTAL_HOME -> teamTotalHome.add(dto);
                case TEAM_TOTAL_AWAY -> teamTotalAway.add(dto);
                case CORRECT_SCORE -> correctScore.add(dto);
                case FIRST_HALF_CORRECT_SCORE -> firstHalfCorrectScore.add(dto);
                case SECOND_HALF_CORRECT_SCORE -> secondHalfCorrectScore.add(dto);
                case RESULT_TOTAL -> resultTotal.add(dto);
                case GOALS -> goals.add(dto);
                case CLEAN_WIN -> cleanWin.add(dto);
                case SCORE_DIFF -> scoreDiff.add(dto);
                case HALF_FULL -> halfFull.add(dto);
                case FIRST_SECOND_HALF -> firstSecondHalf.add(dto);
                case BTTS_RESULT -> bttsResult.add(dto);
            }
        }

        handicaps.sort(handicapComparator());
        halfTimeHandicaps.sort(handicapComparator());
        secondHalfHandicaps.sort(handicapComparator());
        totals.sort(totalComparator());
        halfTimeTotals.sort(totalComparator());
        secondHalfTotals.sort(totalComparator());
        teamTotalHome.sort(totalComparator());
        teamTotalAway.sort(totalComparator());
        correctScore.sort(correctScoreComparator());
        firstHalfCorrectScore.sort(correctScoreComparator());
        secondHalfCorrectScore.sort(correctScoreComparator());

        return MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(matchResult)
                .halfTimeResultMarkets(halfTimeResult)
                .secondHalfResultMarkets(secondHalfResult)
                .handicapMarkets(handicaps)
                .halfTimeHandicapMarkets(halfTimeHandicaps)
                .secondHalfHandicapMarkets(secondHalfHandicaps)
                .totalMarkets(totals)
                .halfTimeTotalMarkets(halfTimeTotals)
                .secondHalfTotalMarkets(secondHalfTotals)
                .teamTotalHomeMarkets(teamTotalHome)
                .teamTotalAwayMarkets(teamTotalAway)
                .correctScoreMarkets(correctScore)
                .firstHalfCorrectScoreMarkets(firstHalfCorrectScore)
                .secondHalfCorrectScoreMarkets(secondHalfCorrectScore)
                .doubleChanceMarkets(doubleChance)
                .halfTimeDoubleChanceMarkets(halfTimeDoubleChance)
                .secondHalfDoubleChanceMarkets(secondHalfDoubleChance)
                .resultTotalMarkets(resultTotal)
                .goalsMarkets(goals)
                .cleanWinMarkets(cleanWin)
                .scoreDiffMarkets(scoreDiff)
                .halfFullMarkets(halfFull)
                .firstSecondHalfMarkets(firstSecondHalf)
                .bttsResultMarkets(bttsResult)
                .build();
    }

    private static MarathonbetExtractedMarkets extractFiltered(JsonNode root, Set<String> models) {
        JsonNode marketsNode = root != null ? root.get("markets") : null;
        if (marketsNode == null || !marketsNode.isObject()) {
            return empty();
        }
        List<MarathonbetMarketDto> matchResult = new ArrayList<>();
        List<MarathonbetMarketDto> handicaps = new ArrayList<>();
        List<MarathonbetMarketDto> totals = new ArrayList<>();
        List<MarathonbetMarketDto> correctScore = new ArrayList<>();
        List<MarathonbetMarketDto> doubleChance = new ArrayList<>();
        List<MarathonbetMarketDto> resultTotal = new ArrayList<>();

        Iterator<String> fieldNames = marketsNode.fieldNames();
        while (fieldNames.hasNext()) {
            JsonNode market = marketsNode.get(fieldNames.next());
            if (market == null || market.isNull()) {
                continue;
            }
            String model = text(market.get("model"));
            if (model == null || !models.contains(model)) {
                continue;
            }
            MarathonbetMarketDto dto = toMarketDto(market);
            if (MarathonbetProdMarketFilter.isIgnoredForProd(dto)) {
                continue;
            }
            OptionalBucket bucket = legacyBucket(model);
            if (bucket == null) {
                continue;
            }
            switch (bucket) {
                case MATCH_RESULT -> matchResult.add(dto);
                case HANDICAP -> handicaps.add(dto);
                case TOTALS -> totals.add(dto);
                case CORRECT_SCORE -> correctScore.add(dto);
                case DOUBLE_CHANCE -> doubleChance.add(dto);
                case RESULT_TOTAL -> resultTotal.add(dto);
            }
        }
        handicaps.sort(handicapComparator());
        totals.sort(totalComparator());
        correctScore.sort(correctScoreComparator());
        return MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(matchResult)
                .halfTimeResultMarkets(List.of())
                .secondHalfResultMarkets(List.of())
                .handicapMarkets(handicaps)
                .halfTimeHandicapMarkets(List.of())
                .secondHalfHandicapMarkets(List.of())
                .totalMarkets(totals)
                .halfTimeTotalMarkets(List.of())
                .secondHalfTotalMarkets(List.of())
                .teamTotalHomeMarkets(List.of())
                .teamTotalAwayMarkets(List.of())
                .correctScoreMarkets(correctScore)
                .firstHalfCorrectScoreMarkets(List.of())
                .secondHalfCorrectScoreMarkets(List.of())
                .doubleChanceMarkets(doubleChance)
                .halfTimeDoubleChanceMarkets(List.of())
                .secondHalfDoubleChanceMarkets(List.of())
                .resultTotalMarkets(resultTotal)
                .goalsMarkets(List.of())
                .cleanWinMarkets(List.of())
                .scoreDiffMarkets(List.of())
                .halfFullMarkets(List.of())
                .firstSecondHalfMarkets(List.of())
                .bttsResultMarkets(List.of())
                .build();
    }

    private enum OptionalBucket {
        MATCH_RESULT, HANDICAP, TOTALS, CORRECT_SCORE, DOUBLE_CHANCE, RESULT_TOTAL
    }

    private static OptionalBucket legacyBucket(String model) {
        return switch (model) {
            case "MTCH_R" -> OptionalBucket.MATCH_RESULT;
            case "MTCH_HB", "MTCH_HB1", "MTCH_HB2" -> OptionalBucket.HANDICAP;
            case "MTCH_TTLG", "MTCH_TTLG1", "MTCH_TTLG2" -> OptionalBucket.TOTALS;
            case "MTCH_DC", "MTCH_DC1", "MTCH_DC2" -> OptionalBucket.DOUBLE_CHANCE;
            case "MTCH_CSDYN", "MTCH_CSW1DYN", "MTCH_CSW2DYN" -> OptionalBucket.CORRECT_SCORE;
            default -> MarathonbetResultTotalModels.isFullTimeResultTotal(model)
                    ? OptionalBucket.RESULT_TOTAL
                    : (model != null && model.startsWith("MTCH_CS") ? OptionalBucket.CORRECT_SCORE : null);
        };
    }

    private static MarathonbetExtractedMarkets empty() {
        return MarathonbetExtractedMarkets.builder()
                .matchResultMarkets(List.of())
                .halfTimeResultMarkets(List.of())
                .secondHalfResultMarkets(List.of())
                .handicapMarkets(List.of())
                .halfTimeHandicapMarkets(List.of())
                .secondHalfHandicapMarkets(List.of())
                .totalMarkets(List.of())
                .halfTimeTotalMarkets(List.of())
                .secondHalfTotalMarkets(List.of())
                .teamTotalHomeMarkets(List.of())
                .teamTotalAwayMarkets(List.of())
                .correctScoreMarkets(List.of())
                .firstHalfCorrectScoreMarkets(List.of())
                .secondHalfCorrectScoreMarkets(List.of())
                .doubleChanceMarkets(List.of())
                .halfTimeDoubleChanceMarkets(List.of())
                .secondHalfDoubleChanceMarkets(List.of())
                .resultTotalMarkets(List.of())
                .goalsMarkets(List.of())
                .cleanWinMarkets(List.of())
                .scoreDiffMarkets(List.of())
                .halfFullMarkets(List.of())
                .firstSecondHalfMarkets(List.of())
                .bttsResultMarkets(List.of())
                .build();
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
        String model = text(market.get("model"));
        return MarathonbetMarketDto.builder()
                .marketId(market.hasNonNull("marketId") ? market.get("marketId").asLong() : null)
                .model(model)
                .name(name)
                .selections(selections)
                .ignoredForProd(MarathonbetProdMarketFilter.isIgnoredForProd(model, name))
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
