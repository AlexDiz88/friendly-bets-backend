package net.friendly_bets.oddsapi;

import net.friendly_bets.models.gameresults.GameResultRecord;
import org.slf4j.Logger;

/**
 * Читабельный вывод несоответствий odds-api команд в лог синхронизации.
 */
final class OddsTeamMappingLog {

    private OddsTeamMappingLog() {
    }

    static void logIssue(
            Logger log,
            int issueNumber,
            GameResultRecord match,
            String side,
            String internalTeamId,
            String apiName,
            Integer apiId,
            boolean idMissing,
            boolean nameMissing
    ) {
        String numberLabel = issueNumber > 0 ? "#" + issueNumber : "—";
        String league = dashIfBlank(match != null ? match.getLeagueCode() : null);
        String season = dashIfBlank(match != null ? match.getSeason() : null);
        String matchday = match != null ? "md" + match.getMatchday() : "md—";
        String matchId = dashIfBlank(match != null ? match.getId() : null);
        String internal = dashIfBlank(internalTeamId);
        String oddsId = apiId != null && apiId > 0 ? String.valueOf(apiId) : "—";

        log.warn(
                """
                ┌─ odds-api team mapping {} ─────────────────────────────
                │ context   : {} · {} · {} · season {}
                │ side      : {}
                │ internal  : {}
                │ odds-api  : {} (id={})
                │ missing   : {}
                └───────────────────────────────────────────────────────""",
                numberLabel,
                matchId,
                league,
                matchday,
                season,
                side.toUpperCase(),
                internal,
                apiName,
                oddsId,
                formatMissing(idMissing, nameMissing)
        );
    }

    static void logSyncSummary(
            Logger log,
            int issueCount,
            String leagueCode,
            int matchday,
            String season
    ) {
        if (issueCount <= 0) {
            return;
        }
        log.warn(
                """
                ══ odds-api sync summary: {} team mapping issue(s) · {} · md{} · season {} ══""",
                issueCount,
                dashIfBlank(leagueCode),
                matchday,
                dashIfBlank(season)
        );
    }

    private static String formatMissing(boolean idMissing, boolean nameMissing) {
        if (idMissing && nameMissing) {
            return "ID alias, name alias";
        }
        if (idMissing) {
            return "ID alias";
        }
        if (nameMissing) {
            return "name alias";
        }
        return "—";
    }

    private static String dashIfBlank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
