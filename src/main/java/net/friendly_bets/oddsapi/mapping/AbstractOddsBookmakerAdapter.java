package net.friendly_bets.oddsapi.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.oddsapi.OddsBttsScope;
import net.friendly_bets.oddsapi.OddsCorrectScoreUtils;
import net.friendly_bets.oddsapi.OddsExactTotalGoalsParser;
import net.friendly_bets.oddsapi.OddsHalfLineSemanticMapper;
import net.friendly_bets.oddsapi.OddsHandicapLine;
import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsMarketParser;
import net.friendly_bets.oddsapi.OddsMatchContext;
import net.friendly_bets.oddsapi.OddsSelectionBetTitleMapper;
import net.friendly_bets.oddsapi.OddsSelectionCode;
import net.friendly_bets.oddsapi.OddsSelectionNormalizer;
import net.friendly_bets.oddsapi.ParsedOddsMarket;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Базовый адаптер этапа 1: JSON одной БК → {@link MappedOddsQuote} с каноническим {@link BetTitle}.
 * Данные других букмекеров здесь недоступны и не используются.
 * <p>
 * Форы: одна строка API {@code {hdp, home, away}} → до двух ставок; правило знака {@code hdp}
 * для гостей — {@link #invertAwayHandicapSign(String)} (переопределяют Xbet/Bet365).
 */
public abstract class AbstractOddsBookmakerAdapter implements OddsBookmakerAdapter {

    @Override
    public List<MappedOddsQuote> mapMarkets(List<OddsApiMarketDto> markets, OddsMatchContext matchContext) {
        List<MappedOddsQuote> result = new ArrayList<>();
        if (markets == null) {
            return result;
        }
        for (OddsApiMarketDto dto : markets) {
            if (dto == null || dto.getName() == null) {
                continue;
            }
            OddsMarketCategory category = resolveCategory(dto.getName());
            if (category == OddsMarketCategory.EXCLUDED || category == OddsMarketCategory.OTHER) {
                continue;
            }
            List<ParsedOddsMarket> parsed = OddsMarketParser.parseAndFilter(List.of(dto));
            for (ParsedOddsMarket market : parsed) {
                OddsBttsScope bttsScope = category == OddsMarketCategory.BTTS
                        ? OddsBttsScope.fromMarketName(market.getName())
                        : null;
                for (ParsedOddsMarket.ParsedOddsLine line : market.getLines()) {
                    String rawRow = OddsRawRowFormatter.fromParsedLine(
                            market.getName(), line.getLine(), line.getPrices());
                    if (category == OddsMarketCategory.HANDICAP) {
                        result.addAll(mapHandicapRow(market.getName(), line, rawRow));
                    } else if (category == OddsMarketCategory.EXACT_TOTAL_GOALS) {
                        result.addAll(mapExactTotalGoalsRow(market.getName(), line, rawRow));
                    } else if (category == OddsMarketCategory.CORRECT_SCORE) {
                        result.addAll(mapCorrectScoreRow(market.getName(), line, rawRow, category));
                    } else {
                        result.addAll(mapStandardRow(
                                market.getName(), line, rawRow, category, bttsScope, matchContext));
                    }
                }
            }
        }
        return result;
    }

    protected OddsMarketCategory resolveCategory(String marketName) {
        return OddsMarketCatalog.resolveCategory(marketName);
    }

    /** Инверсия знака {@code hdp} для гостей — только у 1xbet; Bet365 переопределяет в {@code false}. */
    protected boolean invertAwayHandicapSign(String marketName) {
        return false;
    }

    private List<MappedOddsQuote> mapHandicapRow(
            String marketName,
            ParsedOddsMarket.ParsedOddsLine line,
            String rawRow
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        String apiLine = line.getLine();
        if (OddsHalfLineSemanticMapper.isIgnoredHandicapApiLine(apiLine)) {
            return List.of();
        }
        Map<OddsSelectionCode, String> normalized = new LinkedHashMap<>();
        OddsSelectionNormalizer.normalizeLinePrices(
                OddsMarketCategory.HANDICAP, line.getPrices(), null, normalized);

        MappedOddsQuote homeQuote = mapHandicapSide(
                marketName, rawRow, apiLine, true, normalized.get(OddsSelectionCode.HOME));
        MappedOddsQuote awayQuote = mapHandicapSide(
                marketName, rawRow, apiLine, false, normalized.get(OddsSelectionCode.AWAY));

        if (homeQuote.getMappingStatus() == OddsMappingStatus.REJECTED
                && awayQuote.getMappingStatus() == OddsMappingStatus.REJECTED) {
            return List.of();
        }
        if (homeQuote.isOk()) {
            quotes.add(homeQuote);
        }
        if (awayQuote.isOk()) {
            quotes.add(awayQuote);
        }
        return quotes;
    }

    private MappedOddsQuote mapHandicapSide(
            String marketName,
            String rawRow,
            String apiLine,
            boolean home,
            String odds
    ) {
        String selectionCode = home ? "HOME" : "AWAY";
        if (odds == null || odds.isBlank()) {
            return reject(marketName, rawRow, OddsMarketCategory.HANDICAP,
                    OddsRejectReason.HANDICAP_ROW_INCOMPLETE, "missing " + selectionCode);
        }
        double effective = OddsHandicapLine.effectiveLine(apiLine, home, invertAwayHandicapSign(marketName));
        if (OddsHandicapLine.isImplausibleQuote(effective, odds)) {
            return reject(marketName, rawRow, OddsMarketCategory.HANDICAP,
                    OddsRejectReason.HANDICAP_IMPLAUSIBLE,
                    selectionCode + " line=" + OddsHandicapLine.formatSortKey(effective) + " odds=" + odds);
        }
        String lineForRow = OddsHandicapLine.formatSortKey(effective);
        OddsLineRow row = OddsLineRow.builder()
                .line(lineForRow)
                .selectionCode(selectionCode)
                .build();
        try {
            BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(
                    OddsMarketCategory.HANDICAP.name(), row);
            return MappedOddsQuote.builder()
                    .bookmaker(bookmakerKey())
                    .marketName(marketName)
                    .rawRowJson(rawRow)
                    .category(OddsMarketCategory.HANDICAP)
                    .betTitle(betTitle)
                    .odds(odds)
                    .mappingStatus(OddsMappingStatus.OK)
                    .selectionCode(selectionCode)
                    .line(lineForRow)
                    .build();
        } catch (BadRequestException e) {
            return reject(marketName, rawRow, OddsMarketCategory.HANDICAP,
                    OddsRejectReason.BET_TITLE_UNMAPPED, selectionCode + ": " + e.getMessage());
        }
    }

    private List<MappedOddsQuote> mapStandardRow(
            String marketName,
            ParsedOddsMarket.ParsedOddsLine line,
            String rawRow,
            OddsMarketCategory category,
            OddsBttsScope bttsScope,
            OddsMatchContext matchContext
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        Map<OddsSelectionCode, String> normalized = new LinkedHashMap<>();
        OddsSelectionNormalizer.normalizeLinePrices(category, line.getPrices(), matchContext, normalized);

        if (normalized.isEmpty()) {
            quotes.add(reject(marketName, rawRow, category,
                    OddsRejectReason.SELECTION_UNMAPPED, "no normalized selections"));
            return quotes;
        }

        String canonicalLine = line.getLine();
        if (categoryUsesLine(category) && canonicalLine != null) {
            canonicalLine = OddsHandicapLine.canonicalApiLine(canonicalLine);
        }

        double lineValue = parseLineValue(canonicalLine);
        for (Map.Entry<OddsSelectionCode, String> price : normalized.entrySet()) {
            Optional<OddsHalfLineSemanticMapper.SemanticBet> semantic =
                    resolveSemanticBet(category, lineValue, price.getKey());
            if (semantic.isPresent()) {
                quotes.add(buildSemanticQuote(marketName, rawRow, semantic.get(), price.getValue()));
                continue;
            }
            String selectionCode = category == OddsMarketCategory.BTTS && bttsScope != null
                    ? bttsScope.selectionCode(price.getKey())
                    : price.getKey().name();
            String lineForRow = categoryUsesLine(category) ? canonicalLine : null;
            OddsLineRow row = OddsLineRow.builder()
                    .line(lineForRow)
                    .selectionCode(selectionCode)
                    .build();
            try {
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(category.name(), row);
                quotes.add(MappedOddsQuote.builder()
                        .bookmaker(bookmakerKey())
                        .marketName(marketName)
                        .rawRowJson(rawRow)
                        .category(category)
                        .betTitle(betTitle)
                        .odds(price.getValue())
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode(selectionCode)
                        .line(lineForRow)
                        .build());
            } catch (BadRequestException e) {
                // Линия/исход вне BetTitleCode — без rejected quote.
            }
        }
        return quotes;
    }

    private static Optional<OddsHalfLineSemanticMapper.SemanticBet> resolveSemanticBet(
            OddsMarketCategory category,
            double line,
            OddsSelectionCode selection
    ) {
        return switch (category) {
            case TOTALS -> OddsHalfLineSemanticMapper.mapMatchTotal(line, selection);
            case TEAM_TOTAL_HOME -> OddsHalfLineSemanticMapper.mapTeamTotal(true, line, selection);
            case TEAM_TOTAL_AWAY -> OddsHalfLineSemanticMapper.mapTeamTotal(false, line, selection);
            default -> Optional.empty();
        };
    }

    private MappedOddsQuote buildSemanticQuote(
            String marketName,
            String rawRow,
            OddsHalfLineSemanticMapper.SemanticBet semantic,
            String odds
    ) {
        BetTitle betTitle = BetTitle.builder()
                .code(semantic.code().getCode())
                .label(semantic.code().getLabel())
                .isNot(semantic.isNot())
                .build();
        return MappedOddsQuote.builder()
                .bookmaker(bookmakerKey())
                .marketName(marketName)
                .rawRowJson(rawRow)
                .category(semantic.displayCategory())
                .betTitle(betTitle)
                .odds(odds)
                .mappingStatus(OddsMappingStatus.OK)
                .build();
    }

    private List<MappedOddsQuote> mapExactTotalGoalsRow(
            String marketName,
            ParsedOddsMarket.ParsedOddsLine line,
            String rawRow
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        if (line.getLine() != null && !line.getLine().isBlank()) {
            Map<OddsSelectionCode, String> normalized = new LinkedHashMap<>();
            OddsSelectionNormalizer.normalizeLinePrices(
                    OddsMarketCategory.TOTALS, line.getPrices(), null, normalized);
            double lineValue = parseLineValue(OddsHandicapLine.canonicalApiLine(line.getLine()));
            for (Map.Entry<OddsSelectionCode, String> price : normalized.entrySet()) {
                Optional<OddsHalfLineSemanticMapper.SemanticBet> semantic =
                        OddsHalfLineSemanticMapper.mapMatchTotal(lineValue, price.getKey());
                if (semantic.isPresent()) {
                    quotes.add(buildSemanticQuote(marketName, rawRow, semantic.get(), price.getValue()));
                    continue;
                }
                quotes.addAll(mapStandardTotalsLine(marketName, rawRow, line.getLine(), price));
            }
            return quotes;
        }
        for (Map.Entry<String, String> entry : line.getPrices().entrySet()) {
            Optional<Integer> exactTotal = OddsExactTotalGoalsParser.parseTotalGoalsLabel(entry.getKey());
            if (exactTotal.isEmpty() || exactTotal.get() != 0) {
                continue;
            }
            OddsHalfLineSemanticMapper.SemanticBet semantic = new OddsHalfLineSemanticMapper.SemanticBet(
                    net.friendly_bets.models.enums.BetTitleCode.GAME_SCORE_0_0,
                    false,
                    OddsMarketCategory.GOALS
            );
            quotes.add(buildSemanticQuote(marketName, rawRow, semantic, entry.getValue()));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapStandardTotalsLine(
            String marketName,
            String rawRow,
            String apiLine,
            Map.Entry<OddsSelectionCode, String> price
    ) {
        String canonicalLine = OddsHandicapLine.canonicalApiLine(apiLine);
        OddsLineRow row = OddsLineRow.builder()
                .line(canonicalLine)
                .selectionCode(price.getKey().name())
                .build();
        try {
            BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(
                    OddsMarketCategory.TOTALS.name(), row);
            return List.of(MappedOddsQuote.builder()
                    .bookmaker(bookmakerKey())
                    .marketName(marketName)
                    .rawRowJson(rawRow)
                    .category(OddsMarketCategory.TOTALS)
                    .betTitle(betTitle)
                    .odds(price.getValue())
                    .mappingStatus(OddsMappingStatus.OK)
                    .selectionCode(price.getKey().name())
                    .line(canonicalLine)
                    .build());
        } catch (BadRequestException e) {
            return List.of();
        }
    }

    private static double parseLineValue(String line) {
        if (line == null || line.isBlank()) {
            return 0;
        }
        return Double.parseDouble(line.trim().replace(',', '.'));
    }

    private List<MappedOddsQuote> mapCorrectScoreRow(
            String marketName,
            ParsedOddsMarket.ParsedOddsLine line,
            String rawRow,
            OddsMarketCategory category
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (Map.Entry<String, String> entry : line.getPrices().entrySet()) {
            String selection = entry.getKey();
            if (selection == null || selection.isBlank()) {
                continue;
            }
            if (OddsCorrectScoreUtils.parseScore(selection) == null) {
                continue;
            }
            OddsLineRow row = OddsLineRow.builder()
                    .selectionCode(selection.trim())
                    .build();
            try {
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(category.name(), row);
                quotes.add(MappedOddsQuote.builder()
                        .bookmaker(bookmakerKey())
                        .marketName(marketName)
                        .rawRowJson(rawRow)
                        .category(category)
                        .betTitle(betTitle)
                        .odds(entry.getValue())
                        .mappingStatus(OddsMappingStatus.OK)
                        .selectionCode(selection.trim())
                        .build());
            } catch (BadRequestException e) {
                // Счёт вне whitelist BetTitleCode — пропускаем без issue.
            }
        }
        return quotes;
    }

    private static boolean categoryUsesLine(OddsMarketCategory category) {
        return category == OddsMarketCategory.TOTALS
                || category == OddsMarketCategory.TEAM_TOTAL_HOME
                || category == OddsMarketCategory.TEAM_TOTAL_AWAY;
    }

    private MappedOddsQuote reject(
            String marketName,
            String rawRow,
            OddsMarketCategory category,
            OddsRejectReason reason,
            String detail
    ) {
        return MappedOddsQuote.builder()
                .bookmaker(bookmakerKey())
                .marketName(marketName)
                .rawRowJson(rawRow)
                .category(category)
                .mappingStatus(OddsMappingStatus.REJECTED)
                .rejectReason(reason)
                .rejectDetail(detail)
                .build();
    }
}
