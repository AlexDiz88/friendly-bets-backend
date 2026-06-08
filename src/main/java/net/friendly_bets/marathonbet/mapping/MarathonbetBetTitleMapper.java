package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.marathonbet.MarathonbetProdMarketFilter;
import net.friendly_bets.marathonbet.MarathonbetResultTotalModels;
import net.friendly_bets.marathonbet.MarathonbetSelectionParsing;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.oddsapi.OddsHandicapLine;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsSelectionBetTitleMapper;
import net.friendly_bets.oddsapi.OddsSelectionCode;
import net.friendly_bets.oddsapi.mapping.BetTitleKey;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMappingStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarathonbetBetTitleMapper {

    private static final Pattern SCORE_DIFF_MARGIN = Pattern.compile(
            "в\\s+(\\d+)\\s+гол",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public List<MappedOddsQuote> map(
            MarathonbetExtractedMarkets markets,
            String homeTeam,
            String awayTeam
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        if (markets == null) {
            return quotes;
        }
        append(quotes, markets.getMatchResultMarkets(), m -> mapMatchResult(m, homeTeam, awayTeam, false, false));
        append(quotes, markets.getHalfTimeResultMarkets(), m -> mapMatchResult(m, homeTeam, awayTeam, true, false));
        append(quotes, markets.getSecondHalfResultMarkets(), m -> mapMatchResult(m, homeTeam, awayTeam, false, true));
        append(quotes, markets.getDoubleChanceMarkets(), m -> mapDoubleChance(m, homeTeam, awayTeam, false, false));
        append(quotes, markets.getHalfTimeDoubleChanceMarkets(), m -> mapDoubleChance(m, homeTeam, awayTeam, true, false));
        append(quotes, markets.getSecondHalfDoubleChanceMarkets(), m -> mapDoubleChance(m, homeTeam, awayTeam, false, true));
        append(quotes, markets.getHandicapMarkets(), m -> mapHandicap(m, homeTeam, awayTeam, false, false));
        append(quotes, markets.getHalfTimeHandicapMarkets(), m -> mapHandicap(m, homeTeam, awayTeam, true, false));
        append(quotes, markets.getSecondHalfHandicapMarkets(), m -> mapHandicap(m, homeTeam, awayTeam, false, true));
        append(quotes, markets.getTotalMarkets(), m -> mapTotals(m, false, false));
        append(quotes, markets.getHalfTimeTotalMarkets(), m -> mapTotals(m, true, false));
        append(quotes, markets.getSecondHalfTotalMarkets(), m -> mapTotals(m, false, true));
        append(quotes, markets.getTeamTotalHomeMarkets(), m -> mapTeamTotals(m, true));
        append(quotes, markets.getTeamTotalAwayMarkets(), m -> mapTeamTotals(m, false));
        append(quotes, markets.getCorrectScoreMarkets(), m -> mapCorrectScore(m, homeTeam, awayTeam, false, false));
        append(quotes, markets.getFirstHalfCorrectScoreMarkets(), m -> mapCorrectScore(m, homeTeam, awayTeam, true, false));
        append(quotes, markets.getSecondHalfCorrectScoreMarkets(), m -> mapCorrectScore(m, homeTeam, awayTeam, false, true));
        append(quotes, markets.getResultTotalMarkets(), this::mapResultTotal);
        append(quotes, markets.getGoalsMarkets(), this::mapGoals);
        append(quotes, markets.getCleanWinMarkets(), this::mapCleanWin);
        append(quotes, markets.getScoreDiffMarkets(), this::mapScoreDiff);
        append(quotes, markets.getHalfFullMarkets(), m -> mapHalfFull(m, homeTeam, awayTeam));
        append(quotes, markets.getFirstSecondHalfMarkets(), m -> mapFirstSecondHalf(m, homeTeam, awayTeam));
        append(quotes, markets.getBttsResultMarkets(), this::mapBttsResult);
        return dedupeQuotesByBetTitle(quotes);
    }

    /** Несколько MTCH_HB на одну линию → одна котировка (последняя побеждает). */
    private static List<MappedOddsQuote> dedupeQuotesByBetTitle(List<MappedOddsQuote> quotes) {
        Map<BetTitleKey, MappedOddsQuote> byKey = new LinkedHashMap<>();
        List<MappedOddsQuote> withoutKey = new ArrayList<>();
        for (MappedOddsQuote quote : quotes) {
            BetTitleKey key = quote.betTitleKey();
            if (key == null) {
                withoutKey.add(quote);
                continue;
            }
            byKey.put(key, quote);
        }
        List<MappedOddsQuote> result = new ArrayList<>(byKey.values());
        result.addAll(withoutKey);
        return result;
    }

    private interface MarketMapper {
        List<MappedOddsQuote> map(MarathonbetMarketDto market);
    }

    private static void append(
            List<MappedOddsQuote> quotes,
            List<MarathonbetMarketDto> markets,
            MarketMapper mapper
    ) {
        if (markets == null) {
            return;
        }
        for (MarathonbetMarketDto market : markets) {
            quotes.addAll(mapper.map(market));
        }
    }

    private List<MappedOddsQuote> mapMatchResult(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            BetTitleCode code = resolveMatchResultCode(sel.getName(), homeTeam, awayTeam, firstHalf, secondHalf);
            if (code == null || sel.getOdds() == null) {
                continue;
            }
            String selectionCode = matchResultSelectionCode(code, firstHalf, secondHalf);
            quotes.add(okQuote(market, sel, OddsMarketCategory.MATCH_RESULT, code, null, selectionCode));
        }
        return quotes;
    }

    private static String matchResultSelectionCode(BetTitleCode code, boolean firstHalf, boolean secondHalf) {
        if (!firstHalf && !secondHalf) {
            return switch (code) {
                case HOME_WIN -> "HOME";
                case DRAW -> "DRAW";
                case AWAY_WIN -> "AWAY";
                default -> null;
            };
        }
        return switch (code) {
            case FIRST_HALF_HOME_WIN, SECOND_HALF_HOME_WIN -> "HOME";
            case FIRST_HALF_DRAW, SECOND_HALF_DRAW -> "DRAW";
            case FIRST_HALF_AWAY_WIN, SECOND_HALF_AWAY_WIN -> "AWAY";
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapDoubleChance(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (sel.getOdds() == null) {
                continue;
            }
            Optional<OddsSelectionCode> dc = resolveDoubleChanceSelection(sel.getName(), homeTeam, awayTeam);
            if (dc.isEmpty()) {
                continue;
            }
            BetTitleCode code = resolvePeriodDoubleChance(dc.get(), firstHalf, secondHalf);
            if (code == null) {
                continue;
            }
            quotes.add(okQuote(market, sel, OddsMarketCategory.DOUBLE_CHANCE, code, null, dc.get().name()));
        }
        return quotes;
    }

    private static BetTitleCode resolvePeriodDoubleChance(
            OddsSelectionCode dc,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (!firstHalf && !secondHalf) {
            return switch (dc) {
                case DC_1X -> BetTitleCode.HOME_WIN_OR_DRAW;
                case DC_12 -> BetTitleCode.HOME_OR_AWAY_WIN;
                case DC_X2 -> BetTitleCode.AWAY_WIN_OR_DRAW;
                default -> null;
            };
        }
        if (firstHalf) {
            return switch (dc) {
                case DC_1X -> BetTitleCode.FIRST_HALF_HOME_WIN_OR_DRAW;
                case DC_12 -> BetTitleCode.FIRST_HALF_HOME_OR_AWAY_WIN;
                case DC_X2 -> BetTitleCode.FIRST_HALF_AWAY_WIN_OR_DRAW;
                default -> null;
            };
        }
        return switch (dc) {
            case DC_1X -> BetTitleCode.SECOND_HALF_HOME_WIN_OR_DRAW;
            case DC_12 -> BetTitleCode.SECOND_HALF_HOME_OR_AWAY_WIN;
            case DC_X2 -> BetTitleCode.SECOND_HALF_AWAY_WIN_OR_DRAW;
            default -> null;
        };
    }

    private Optional<OddsSelectionCode> resolveDoubleChanceSelection(
            String name,
            String homeTeam,
            String awayTeam
    ) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        if ("1x".equals(trimmed) || trimmed.contains("1x")) {
            return Optional.of(OddsSelectionCode.DC_1X);
        }
        if ("12".equals(trimmed)) {
            return Optional.of(OddsSelectionCode.DC_12);
        }
        if ("x2".equals(trimmed)) {
            return Optional.of(OddsSelectionCode.DC_X2);
        }
        String[] segments = trimmed.split(" или ");
        boolean hasDraw = false;
        boolean hasHome = false;
        boolean hasAway = false;
        for (String segment : segments) {
            String part = segment.trim();
            if (part.contains("ничья")) {
                hasDraw = true;
            } else if (teamInSegment(part, homeTeam)) {
                hasHome = true;
            } else if (teamInSegment(part, awayTeam)) {
                hasAway = true;
            }
        }
        if (hasHome && hasDraw && !hasAway) {
            return Optional.of(OddsSelectionCode.DC_1X);
        }
        if (hasHome && hasAway && !hasDraw) {
            return Optional.of(OddsSelectionCode.DC_12);
        }
        if (hasDraw && hasAway && !hasHome) {
            return Optional.of(OddsSelectionCode.DC_X2);
        }
        return Optional.empty();
    }

    private boolean teamInSegment(String segment, String team) {
        if (team == null || team.isBlank()) {
            return false;
        }
        String homeNorm = MarathonbetSelectionParsing.normalizeTeam(team);
        String segNorm = MarathonbetSelectionParsing.normalizeTeam(
                segment.replace("(победа)", "").trim()
        );
        return homeNorm != null && segNorm != null && segNorm.contains(homeNorm);
    }

    private List<MappedOddsQuote> mapResultTotal(MarathonbetMarketDto market) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        String model = market.getModel();
        MarathonbetResultTotalModels.ResultLeg leg = MarathonbetResultTotalModels.resultLeg(model);
        if (leg == null) {
            return List.of();
        }
        Double line = MarathonbetSelectionParsing.parseResultTotalLine(market.getName());
        if (line == null) {
            return List.of();
        }
        boolean under = MarathonbetResultTotalModels.isUnder(model);
        BetTitleCode code = resolveResultTotalBetTitle(leg, under, line);
        if (code == null) {
            return List.of();
        }
        OddsMarketCategory category = under
                ? OddsMarketCategory.RESULT_TOTAL_UNDER
                : OddsMarketCategory.RESULT_TOTAL_OVER;
        String lineKey = formatTotalLine(line);
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (sel.getOdds() == null || sel.getName() == null) {
                continue;
            }
            if (!"Да".equalsIgnoreCase(sel.getName().trim())) {
                continue;
            }
            quotes.add(okQuote(market, sel, category, code, lineKey, "YES"));
        }
        return quotes;
    }

    private static BetTitleCode resolveResultTotalBetTitle(
            MarathonbetResultTotalModels.ResultLeg leg,
            boolean under,
            double line
    ) {
        String prefix = switch (leg) {
            case HOME_WIN -> "HOME_WIN";
            case AWAY_WIN -> "AWAY_WIN";
            case DRAW -> "DRAW";
            case HOME_OR_DRAW -> "HOME_WIN_OR_DRAW";
            case AWAY_OR_DRAW -> "AWAY_WIN_OR_DRAW";
            case HOME_OR_AWAY -> "HOME_OR_AWAY";
        };
        String totalPart = under ? "UNDER" : "OVER";
        String suffix = formatLineSuffix(line);
        String enumName = prefix + "_AND_" + totalPart + "_" + suffix;
        try {
            return BetTitleCode.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String formatLineSuffix(double line) {
        if (line == Math.floor(line)) {
            return ((int) line) + "_0";
        }
        return String.valueOf(line).replace('.', '_');
    }

    private BetTitleCode resolveMatchResultCode(
            String name,
            String homeTeam,
            String awayTeam,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if ("Ничья".equalsIgnoreCase(trimmed)) {
            if (firstHalf) {
                return BetTitleCode.FIRST_HALF_DRAW;
            }
            if (secondHalf) {
                return BetTitleCode.SECOND_HALF_DRAW;
            }
            return BetTitleCode.DRAW;
        }
        String homeNorm = MarathonbetSelectionParsing.normalizeTeam(homeTeam);
        String awayNorm = MarathonbetSelectionParsing.normalizeTeam(awayTeam);
        String selNorm = MarathonbetSelectionParsing.normalizeTeam(
                trimmed.replace("(победа)", "").trim()
        );
        if (homeNorm != null && selNorm != null && selNorm.contains(homeNorm)) {
            if (firstHalf) {
                return BetTitleCode.FIRST_HALF_HOME_WIN;
            }
            if (secondHalf) {
                return BetTitleCode.SECOND_HALF_HOME_WIN;
            }
            return BetTitleCode.HOME_WIN;
        }
        if (awayNorm != null && selNorm != null && selNorm.contains(awayNorm)) {
            if (firstHalf) {
                return BetTitleCode.FIRST_HALF_AWAY_WIN;
            }
            if (secondHalf) {
                return BetTitleCode.SECOND_HALF_AWAY_WIN;
            }
            return BetTitleCode.AWAY_WIN;
        }
        return null;
    }

    private List<MappedOddsQuote> mapHandicap(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (isDrawHandicapSelection(sel.getName())) {
                continue;
            }
            Double line = MarathonbetSelectionParsing.parseHandicapLine(sel.getName());
            if (line == null || sel.getOdds() == null) {
                continue;
            }
            boolean home = isHomeSelection(sel.getName(), homeTeam, awayTeam);
            String selectionCode = home ? "HOME" : "AWAY";
            String lineKey = OddsHandicapLine.formatSortKey(line);
            BetTitleCode code = resolvePeriodHandicap(line, home, firstHalf, secondHalf);
            if (code == null) {
                continue;
            }
            OddsMarketCategory category = (firstHalf || secondHalf)
                    ? OddsMarketCategory.PERIOD_HANDICAP
                    : OddsMarketCategory.HANDICAP;
            quotes.add(okQuote(market, sel, category, code, lineKey, selectionCode));
        }
        return quotes;
    }

    private static BetTitleCode resolvePeriodHandicap(
            double line,
            boolean home,
            boolean firstHalf,
            boolean secondHalf
    ) {
        String period = firstHalf ? "FIRST_HALF_" : (secondHalf ? "SECOND_HALF_" : "");
        String side = home ? "HANDICAP_HOME" : "HANDICAP_AWAY";
        String suffix = handicapSuffix(line);
        try {
            return BetTitleCode.valueOf(period + side + suffix);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String handicapSuffix(double effectiveLine) {
        if (Math.abs(effectiveLine) < 1e-9) {
            return "_0";
        }
        String sign = effectiveLine > 0 ? "PLUS" : "MINUS";
        String value = formatLineSuffix(Math.abs(effectiveLine));
        return "_" + sign + "_" + value;
    }

    private static boolean isDrawHandicapSelection(String selectionName) {
        if (selectionName == null || selectionName.isBlank()) {
            return false;
        }
        int paren = selectionName.indexOf('(');
        String prefix = paren > 0 ? selectionName.substring(0, paren).trim() : selectionName.trim();
        return "ничья".equalsIgnoreCase(prefix);
    }

    private boolean isHomeSelection(String selectionName, String homeTeam, String awayTeam) {
        if (selectionName == null) {
            return true;
        }
        int paren = selectionName.indexOf('(');
        String prefix = paren > 0 ? selectionName.substring(0, paren).trim() : selectionName;
        String homeNorm = MarathonbetSelectionParsing.normalizeTeam(homeTeam);
        String awayNorm = MarathonbetSelectionParsing.normalizeTeam(awayTeam);
        String prefixNorm = MarathonbetSelectionParsing.normalizeTeam(prefix);
        if (homeNorm != null && prefixNorm != null && prefixNorm.contains(homeNorm)) {
            return true;
        }
        if (awayNorm != null && prefixNorm != null && prefixNorm.contains(awayNorm)) {
            return false;
        }
        return true;
    }

    private List<MappedOddsQuote> mapTotals(
            MarathonbetMarketDto market,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            Double line = MarathonbetSelectionParsing.parseTotalLine(sel.getName());
            if (line == null || sel.getOdds() == null) {
                continue;
            }
            String selectionCode = MarathonbetSelectionParsing.isTotalOver(sel.getName()) ? "OVER" : "UNDER";
            String lineKey = formatTotalLine(line);
            BetTitleCode code = resolvePeriodTotal(line, selectionCode, firstHalf, secondHalf);
            if (code == null) {
                continue;
            }
            OddsMarketCategory category = (firstHalf || secondHalf)
                    ? OddsMarketCategory.HALF_TOTALS
                    : OddsMarketCategory.TOTALS;
            quotes.add(okQuote(market, sel, category, code, lineKey, selectionCode));
        }
        return quotes;
    }

    private static BetTitleCode resolvePeriodTotal(
            double line,
            String selectionCode,
            boolean firstHalf,
            boolean secondHalf
    ) {
        String period = firstHalf ? "FIRST_HALF_" : (secondHalf ? "SECOND_HALF_" : "");
        boolean over = "OVER".equals(selectionCode);
        String suffix = formatLineSuffix(line);
        String name = period + "TOTAL_" + (over ? "OVER" : "UNDER") + "_" + suffix;
        try {
            return BetTitleCode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<MappedOddsQuote> mapTeamTotals(MarathonbetMarketDto market, boolean home) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        OddsMarketCategory category = home ? OddsMarketCategory.TEAM_TOTAL_HOME : OddsMarketCategory.TEAM_TOTAL_AWAY;
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            Double line = MarathonbetSelectionParsing.parseTotalLine(sel.getName());
            if (line == null || sel.getOdds() == null) {
                continue;
            }
            String selectionCode = MarathonbetSelectionParsing.isTotalOver(sel.getName()) ? "OVER" : "UNDER";
            String lineKey = formatTotalLine(line);
            try {
                OddsLineRow row = OddsLineRow.builder()
                        .selectionCode(selectionCode)
                        .line(lineKey)
                        .build();
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(category.name(), row);
                quotes.add(okQuote(market, sel, category, betTitle, lineKey, selectionCode));
            } catch (Exception ignored) {
                // unsupported line
            }
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapCorrectScore(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam,
            boolean firstHalf,
            boolean secondHalf
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        OddsMarketCategory category = firstHalf
                ? OddsMarketCategory.FIRST_HALF_CORRECT_SCORE
                : (secondHalf ? OddsMarketCategory.SECOND_HALF_CORRECT_SCORE : OddsMarketCategory.CORRECT_SCORE);
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            int[] score = MarathonbetSelectionParsing.parseCorrectScoreHomeAway(
                    sel.getName(), homeTeam, awayTeam);
            if (score == null || sel.getOdds() == null) {
                continue;
            }
            String selection = score[0] + "-" + score[1];
            BetTitleCode code = resolvePeriodCorrectScore(score[0], score[1], firstHalf, secondHalf);
            if (code == null) {
                continue;
            }
            BetTitle betTitle = BetTitle.builder()
                    .code(code.getCode())
                    .label(code.getLabel())
                    .isNot(false)
                    .build();
            quotes.add(okQuote(market, sel, category, betTitle, null, selection));
        }
        return quotes;
    }

    private static BetTitleCode resolvePeriodCorrectScore(
            int home,
            int away,
            boolean firstHalf,
            boolean secondHalf
    ) {
        String prefix = firstHalf ? "FIRST_HALF_SCORE_" : (secondHalf ? "SECOND_HALF_SCORE_" : "GAME_SCORE_");
        try {
            return BetTitleCode.valueOf(prefix + home + "_" + away);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<MappedOddsQuote> mapGoals(MarathonbetMarketDto market) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        BetTitleCode code = goalsCodeForModel(market.getModel());
        if (code == null) {
            return List.of();
        }
        return mapYesNo(market, code, OddsMarketCategory.GOALS);
    }

    private static BetTitleCode goalsCodeForModel(String model) {
        if (model == null) {
            return null;
        }
        return switch (model) {
            case "MTCH_T12G" -> BetTitleCode.BOTH_TEAMS_SCORE;
            case "MTCH_T1G" -> BetTitleCode.HOME_TEAM_SCORES;
            case "MTCH_T2G" -> BetTitleCode.AWAY_TEAM_SCORES;
            case "MTCH_T12G1OR2" -> BetTitleCode.ANY_TEAM_WILL_SCORE;
            case "MTCH_T12G1" -> BetTitleCode.BOTH_TEAMS_SCORE_1ST_HALF;
            case "MTCH_T12G2" -> BetTitleCode.BOTH_TEAMS_SCORE_2ND_HALF;
            case "MTCH_T1G1" -> BetTitleCode.HOME_SCORES_1ST_HALF;
            case "MTCH_T1G2" -> BetTitleCode.HOME_SCORES_2ND_HALF;
            case "MTCH_T1G12" -> BetTitleCode.HOME_SCORES_BOTH_HALVES;
            case "MTCH_T2G1" -> BetTitleCode.AWAY_SCORES_1ST_HALF;
            case "MTCH_T2G2" -> BetTitleCode.AWAY_SCORES_2ND_HALF;
            case "MTCH_T2G12" -> BetTitleCode.AWAY_SCORES_BOTH_HALVES;
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapCleanWin(MarathonbetMarketDto market) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        BetTitleCode code = switch (market.getModel()) {
            case "MTCH_T1W0" -> BetTitleCode.CLEAN_WIN_HOME;
            case "MTCH_T2W0" -> BetTitleCode.CLEAN_WIN_AWAY;
            case "MTCH_TEW0" -> BetTitleCode.CLEAN_WIN_ANY;
            default -> null;
        };
        if (code == null) {
            return List.of();
        }
        return mapYesNo(market, code, OddsMarketCategory.CLEAN_WIN);
    }

    private List<MappedOddsQuote> mapScoreDiff(MarathonbetMarketDto market) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        BetTitleCode code = scoreDiffCode(market.getModel(), market.getName());
        if (code == null) {
            return List.of();
        }
        return mapYesNo(market, code, OddsMarketCategory.WIN_GOAL_DIFFERENCE);
    }

    private static BetTitleCode scoreDiffCode(String model, String marketName) {
        if (model == null || marketName == null) {
            return null;
        }
        Matcher m = SCORE_DIFF_MARGIN.matcher(marketName);
        if (!m.find()) {
            return null;
        }
        int margin = Integer.parseInt(m.group(1));
        String suffix = switch (margin) {
            case 1 -> "_1";
            case 2 -> "_2";
            case 3 -> "_3";
            default -> null;
        };
        if (suffix == null) {
            return null;
        }
        String prefix = switch (model) {
            case "MTCH_T1WM" -> "GOALS_DIFF_HOME_WIN";
            case "MTCH_T2WM" -> "GOALS_DIFF_AWAY_WIN";
            case "MTCH_TEWM" -> "GOALS_DIFF_HOME_OR_AWAY_WIN";
            default -> null;
        };
        if (prefix == null) {
            return null;
        }
        try {
            return BetTitleCode.valueOf(prefix + suffix);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<MappedOddsQuote> mapBttsResult(MarathonbetMarketDto market) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        BetTitleCode code = bttsResultCode(market.getModel());
        if (code == null) {
            return List.of();
        }
        return mapYesNo(market, code, OddsMarketCategory.RESULT_BTTS);
    }

    private static BetTitleCode bttsResultCode(String model) {
        if (model == null) {
            return null;
        }
        return switch (model) {
            case "MTCH_T12GW1" -> BetTitleCode.HOME_WIN_AND_BOTH_TEAMS_SCORE;
            case "MTCH_T12GWX" -> BetTitleCode.DRAW_AND_BOTH_TEAMS_SCORE;
            case "MTCH_T12GWX2" -> BetTitleCode.AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE;
            case "MTCH_T12GW1X" -> BetTitleCode.HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE;
            case "MTCH_T12GW12" -> BetTitleCode.HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE;
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapYesNo(
            MarathonbetMarketDto market,
            BetTitleCode code,
            OddsMarketCategory category
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (sel.getOdds() == null || sel.getName() == null) {
                continue;
            }
            String name = sel.getName().trim();
            Boolean yes = null;
            if ("Да".equalsIgnoreCase(name)) {
                yes = true;
            } else if ("Нет".equalsIgnoreCase(name)) {
                yes = false;
            }
            if (yes == null) {
                continue;
            }
            BetTitle betTitle = BetTitle.builder()
                    .code(code.getCode())
                    .label(code.getLabel())
                    .isNot(!yes)
                    .build();
            quotes.add(okQuote(market, sel, category, betTitle, null, yes ? "YES" : "NO"));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapHalfFull(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (sel.getOdds() == null || sel.getName() == null) {
                continue;
            }
            BetTitleCode code = resolveHalfFullCode(sel.getName(), homeTeam, awayTeam, true);
            if (code == null) {
                continue;
            }
            quotes.add(okQuote(market, sel, OddsMarketCategory.HALF_FULL, code, null, sel.getName()));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapFirstSecondHalf(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            if (sel.getOdds() == null || sel.getName() == null) {
                continue;
            }
            BetTitleCode code = resolveHalfFullCode(sel.getName(), homeTeam, awayTeam, false);
            if (code == null) {
                continue;
            }
            quotes.add(okQuote(market, sel, OddsMarketCategory.FIRST_SECOND_HALF, code, null, sel.getName()));
        }
        return quotes;
    }

    private BetTitleCode resolveHalfFullCode(
            String selection,
            String homeTeam,
            String awayTeam,
            boolean halfFull
    ) {
        String[] parts = selection.split("\\+");
        if (parts.length != 2) {
            return null;
        }
        ResultLeg first = parseResultLeg(parts[0], homeTeam, awayTeam, halfFull, true);
        ResultLeg second = parseResultLeg(parts[1], homeTeam, awayTeam, halfFull, false);
        if (first == null || second == null) {
            return null;
        }
        String enumName = (halfFull ? "HALF_FULL_" : "FIRST_SECOND_")
                + first.name() + "_" + second.name();
        try {
            return BetTitleCode.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private enum ResultLeg { HOME, DRAW, AWAY }

    private ResultLeg parseResultLeg(
            String segment,
            String homeTeam,
            String awayTeam,
            boolean halfFull,
            boolean firstSegment
    ) {
        String trimmed = segment.trim().toLowerCase(Locale.ROOT);
        if (trimmed.contains("ничья")) {
            return ResultLeg.DRAW;
        }
        if (teamInSegment(trimmed, homeTeam) && trimmed.contains("победа")) {
            return ResultLeg.HOME;
        }
        if (teamInSegment(trimmed, awayTeam) && trimmed.contains("победа")) {
            return ResultLeg.AWAY;
        }
        if (halfFull) {
            if (firstSegment && trimmed.contains("1-м тайме") && teamInSegment(trimmed, homeTeam)) {
                return ResultLeg.HOME;
            }
            if (firstSegment && trimmed.contains("1-м тайме") && teamInSegment(trimmed, awayTeam)) {
                return ResultLeg.AWAY;
            }
            if (!firstSegment && trimmed.contains("матче") && teamInSegment(trimmed, homeTeam)) {
                return ResultLeg.HOME;
            }
            if (!firstSegment && trimmed.contains("матче") && teamInSegment(trimmed, awayTeam)) {
                return ResultLeg.AWAY;
            }
        } else {
            if (firstSegment && trimmed.contains("1-м тайме") && teamInSegment(trimmed, homeTeam)) {
                return ResultLeg.HOME;
            }
            if (firstSegment && trimmed.contains("1-м тайме") && teamInSegment(trimmed, awayTeam)) {
                return ResultLeg.AWAY;
            }
            if (!firstSegment && trimmed.contains("2-м тайме") && teamInSegment(trimmed, homeTeam)) {
                return ResultLeg.HOME;
            }
            if (!firstSegment && trimmed.contains("2-м тайме") && teamInSegment(trimmed, awayTeam)) {
                return ResultLeg.AWAY;
            }
        }
        return null;
    }

    private static String formatTotalLine(double line) {
        if (line == Math.floor(line)) {
            return String.valueOf((int) line);
        }
        return String.valueOf(line);
    }

    private MappedOddsQuote okQuote(
            MarathonbetMarketDto market,
            MarathonbetMarketSelectionDto sel,
            OddsMarketCategory category,
            BetTitleCode code,
            String line
    ) {
        return okQuote(market, sel, category, code, line, null);
    }

    private MappedOddsQuote okQuote(
            MarathonbetMarketDto market,
            MarathonbetMarketSelectionDto sel,
            OddsMarketCategory category,
            BetTitleCode code,
            String line,
            String selectionCode
    ) {
        BetTitle betTitle = BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(false)
                .build();
        return okQuote(market, sel, category, betTitle, line, selectionCode);
    }

    private MappedOddsQuote okQuote(
            MarathonbetMarketDto market,
            MarathonbetMarketSelectionDto sel,
            OddsMarketCategory category,
            BetTitle betTitle
    ) {
        return okQuote(market, sel, category, betTitle, null);
    }

    private MappedOddsQuote okQuote(
            MarathonbetMarketDto market,
            MarathonbetMarketSelectionDto sel,
            OddsMarketCategory category,
            BetTitle betTitle,
            String line
    ) {
        return okQuote(market, sel, category, betTitle, line, null);
    }

    private MappedOddsQuote okQuote(
            MarathonbetMarketDto market,
            MarathonbetMarketSelectionDto sel,
            OddsMarketCategory category,
            BetTitle betTitle,
            String line,
            String selectionCode
    ) {
        String odds = sel.getOdds() != null ? sel.getOdds().toPlainString() : null;
        String sourcePath = "marathonbet/" + market.getModel()
                + (sel.getSelId() != null ? "/" + sel.getSelId() : "/" + sel.getName());
        return MappedOddsQuote.builder()
                .bookmaker(MarathonbetBookmaker.KEY)
                .marketName(market.getName())
                .category(category)
                .betTitle(betTitle)
                .odds(odds)
                .mappingStatus(OddsMappingStatus.OK)
                .selectionCode(selectionCode != null ? selectionCode : sel.getName())
                .line(line)
                .sourcePath(sourcePath)
                .build();
    }
}
