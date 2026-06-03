package net.friendly_bets.marathonbet.mapping;

import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.marathonbet.MarathonbetProdMarketFilter;
import net.friendly_bets.marathonbet.MarathonbetSelectionParsing;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.oddsapi.OddsCorrectScoreUtils;
import net.friendly_bets.oddsapi.OddsHandicapLine;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsSelectionBetTitleMapper;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMappingStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class MarathonbetBetTitleMapper {

    public List<MappedOddsQuote> map(
            MarathonbetExtractedMarkets markets,
            String homeTeam,
            String awayTeam
    ) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        if (markets == null) {
            return quotes;
        }
        for (MarathonbetMarketDto market : markets.getMatchResultMarkets()) {
            quotes.addAll(mapMatchResult(market, homeTeam, awayTeam));
        }
        for (MarathonbetMarketDto market : markets.getHandicapMarkets()) {
            quotes.addAll(mapHandicap(market, homeTeam, awayTeam));
        }
        for (MarathonbetMarketDto market : markets.getTotalMarkets()) {
            quotes.addAll(mapTotals(market));
        }
        for (MarathonbetMarketDto market : markets.getCorrectScoreMarkets()) {
            quotes.addAll(mapCorrectScore(market, homeTeam, awayTeam));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapMatchResult(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            BetTitleCode code = resolveMatchResultCode(sel.getName(), homeTeam, awayTeam);
            if (code == null || sel.getOdds() == null) {
                continue;
            }
            quotes.add(okQuote(market, sel, OddsMarketCategory.MATCH_RESULT, code, null));
        }
        return quotes;
    }

    private BetTitleCode resolveMatchResultCode(String name, String homeTeam, String awayTeam) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if ("Ничья".equalsIgnoreCase(trimmed)) {
            return BetTitleCode.DRAW;
        }
        String homeNorm = MarathonbetSelectionParsing.normalizeTeam(homeTeam);
        String awayNorm = MarathonbetSelectionParsing.normalizeTeam(awayTeam);
        String selNorm = MarathonbetSelectionParsing.normalizeTeam(
                trimmed.replace("(победа)", "").trim()
        );
        if (homeNorm != null && selNorm != null && selNorm.contains(homeNorm)) {
            return BetTitleCode.HOME_WIN;
        }
        if (awayNorm != null && selNorm != null && selNorm.contains(awayNorm)) {
            return BetTitleCode.AWAY_WIN;
        }
        return null;
    }

    private List<MappedOddsQuote> mapHandicap(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            Double line = MarathonbetSelectionParsing.parseHandicapLine(sel.getName());
            if (line == null || sel.getOdds() == null) {
                continue;
            }
            boolean home = isHomeSelection(sel.getName(), homeTeam, awayTeam);
            String selectionCode = home ? "HOME" : "AWAY";
            // У Marathon в подписи исхода уже финальный знак (ЮАР (+1)); не инвертировать как у odds-api Spread.
            String lineKey = OddsHandicapLine.formatSortKey(line);
            try {
                OddsLineRow row = OddsLineRow.builder()
                        .selectionCode(selectionCode)
                        .line(lineKey)
                        .build();
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(
                        OddsMarketCategory.HANDICAP.name(), row);
                quotes.add(okQuote(market, sel, OddsMarketCategory.HANDICAP, betTitle, lineKey, selectionCode));
            } catch (Exception ignored) {
                // line outside BetTitle whitelist
            }
        }
        return quotes;
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

    private List<MappedOddsQuote> mapTotals(MarathonbetMarketDto market) {
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
            try {
                OddsLineRow row = OddsLineRow.builder()
                        .selectionCode(selectionCode)
                        .line(lineKey)
                        .build();
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(
                        OddsMarketCategory.TOTALS.name(), row);
                quotes.add(okQuote(market, sel, OddsMarketCategory.TOTALS, betTitle, lineKey, selectionCode));
            } catch (Exception ignored) {
                // unsupported total line
            }
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapCorrectScore(
            MarathonbetMarketDto market,
            String homeTeam,
            String awayTeam
    ) {
        if (MarathonbetProdMarketFilter.isIgnoredForProd(market)) {
            return List.of();
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MarathonbetMarketSelectionDto sel : market.getSelections()) {
            int[] score = MarathonbetSelectionParsing.parseCorrectScoreHomeAway(
                    sel.getName(), homeTeam, awayTeam);
            if (score == null || sel.getOdds() == null) {
                continue;
            }
            String selection = score[0] + "-" + score[1];
            try {
                OddsLineRow row = OddsLineRow.builder().selectionCode(selection).build();
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(
                        OddsMarketCategory.CORRECT_SCORE.name(), row);
                quotes.add(okQuote(market, sel, OddsMarketCategory.CORRECT_SCORE, betTitle));
            } catch (Exception ignored) {
                // score outside whitelist
            }
        }
        return quotes;
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
        BetTitle betTitle = BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(false)
                .build();
        return okQuote(market, sel, category, betTitle, line);
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
