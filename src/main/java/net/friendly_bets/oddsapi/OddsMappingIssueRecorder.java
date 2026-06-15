package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsCrossBookmakerMismatch;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.oddsapi.mapping.OddsRejectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OddsMappingIssueRecorder {

    private static final Logger log = LoggerFactory.getLogger(OddsMappingIssueRecorder.class);

    private final ApiSyncIssueService apiSyncIssueService;

    public void recordMergeResult(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            OddsMergeResult mergeResult
    ) {
        if (mergeResult == null) {
            return;
        }
        for (MappedOddsQuote rejected : mergeResult.getRejectedQuotes()) {
            if (rejected.getRejectReason() == null || !shouldRecordRejected(rejected)) {
                continue;
            }
            String message = buildMessage(rejected);
            log.warn("odds mapping rejected: gameResultId={} oddsApiEventId={} bookmaker={} market={} reason={} {}",
                    match != null ? match.getId() : null,
                    oddsApiEventId(match),
                    rejected.getBookmaker(),
                    rejected.getMarketName(),
                    rejected.getRejectReason(),
                    message);
            recordRejected(match, leagueCode, season, matchday, rejected, message);
        }
        for (OddsCrossBookmakerMismatch mismatch : mergeResult.getMismatches()) {
            String message = formatCrossBookmakerMismatchMessage(mismatch);
            log.warn("odds cross-bookmaker mismatch: gameResultId={} oddsApiEventId={} {}",
                    match != null ? match.getId() : null,
                    oddsApiEventId(match),
                    message);
            apiSyncIssueService.recordOddsQuoteMismatch(match, leagueCode, season, matchday, message);
        }
    }

    static String formatCrossBookmakerMismatchMessage(OddsCrossBookmakerMismatch mismatch) {
        String key = mismatch.getBetTitleKey().storageKey();
        String labelSuffix = "";
        if (mismatch.getBetTitle() != null) {
            String label = mismatch.getBetTitle().getLabel();
            if (label != null && !label.isBlank()) {
                labelSuffix = " (" + label.trim() + ")";
            }
        }
        return "Есть возможные проблемы с кэфами: betTitle=" + key + labelSuffix
                + " " + mismatch.getBookmakerA() + "=" + mismatch.getOddsA()
                + " " + mismatch.getBookmakerB() + "=" + mismatch.getOddsB();
    }

    private static boolean shouldRecordRejected(MappedOddsQuote rejected) {
        OddsRejectReason reason = rejected.getRejectReason();
        if (reason == null) {
            return false;
        }
        return switch (reason) {
            case MARKET_EXCLUDED, MARKET_UNMAPPED, BET_TITLE_UNMAPPED, HANDICAP_ROW_INCOMPLETE,
                    CROSS_BOOKMAKER_MISMATCH -> false;
            case SELECTION_UNMAPPED -> rejected.getCategory() == OddsMarketCategory.DOUBLE_CHANCE
                    || rejected.getCategory() == OddsMarketCategory.MATCH_RESULT;
        };
    }

    private void recordRejected(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            MappedOddsQuote rejected,
            String message
    ) {
        OddsRejectReason reason = rejected.getRejectReason();
        if (reason == OddsRejectReason.MARKET_UNMAPPED) {
            apiSyncIssueService.recordOddsMarketUnmapped(
                    match, leagueCode, season, matchday,
                    rejected.getBookmaker(), rejected.getMarketName(), message);
        } else if (reason == OddsRejectReason.SELECTION_UNMAPPED
                || reason == OddsRejectReason.BET_TITLE_UNMAPPED) {
            apiSyncIssueService.recordOddsSelectionUnmapped(
                    match, leagueCode, season, matchday,
                    rejected.getBookmaker(), rejected.getMarketName(), message);
        } else if (reason == OddsRejectReason.HANDICAP_ROW_INCOMPLETE) {
            apiSyncIssueService.recordOddsQuoteRejected(
                    match, leagueCode, season, matchday,
                    rejected.getBookmaker(), rejected.getMarketName(), message);
        }
    }

    private static Long oddsApiEventId(GameResultRecord match) {
        if (match == null || match.getOddsApiEventId() == null || match.getOddsApiEventId() <= 0) {
            return null;
        }
        return match.getOddsApiEventId();
    }

    private static String buildMessage(MappedOddsQuote rejected) {
        String raw = rejected.getRawRowJson();
        if (raw != null && raw.length() > 200) {
            raw = raw.substring(0, 200) + "…";
        }
        return rejected.getRejectReason()
                + (rejected.getRejectDetail() != null ? ": " + rejected.getRejectDetail() : "")
                + (raw != null ? " row=" + raw : "");
    }
}
