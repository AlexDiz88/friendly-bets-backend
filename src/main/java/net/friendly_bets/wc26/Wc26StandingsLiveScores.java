package net.friendly_bets.wc26;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.dto.Wc26ScheduleMatchDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.fifa.FifaMatchParser;
import net.friendly_bets.gameresults.MatchStatuses;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Live-счёт матча (home:away) для строк таблицы. */
public final class Wc26StandingsLiveScores {

    private static final String DEFAULT_LIVE_SCORE = "0:0";

    private Wc26StandingsLiveScores() {
    }

    /** primary (FIFA calendar) перекрывает fallback (расписание/4score). */
    public static Map<String, String> merge(Map<String, String> fallback, Map<String, String> primary) {
        Map<String, String> merged = new HashMap<>();
        if (fallback != null) {
            merged.putAll(fallback);
        }
        if (primary != null) {
            merged.putAll(primary);
        }
        return merged;
    }

    public static Map<String, String> byTeam(Wc26SchedulePageDto schedule) {
        Map<String, String> result = new HashMap<>();
        if (schedule == null || schedule.getMatches() == null) {
            return result;
        }
        for (Wc26ScheduleMatchDto match : schedule.getMatches()) {
            if (!isLive(match)) {
                continue;
            }
            String home = normalizeFifaCode(match.getHome());
            String away = normalizeFifaCode(match.getAway());
            if (home == null || away == null) {
                continue;
            }
            int[] goals = parseMatchGoals(match.getScoreView());
            result.put(home, goals[0] + ":" + goals[1]);
            result.put(away, goals[1] + ":" + goals[0]);
        }
        return result;
    }

    public static Map<String, String> byTeamFromFifaCalendar(List<JsonNode> matches) {
        Map<String, String> result = new HashMap<>();
        if (matches == null) {
            return result;
        }
        for (JsonNode match : matches) {
            if (!FifaMatchParser.isGroupStage(match) || !FifaMatchParser.isLive(match)) {
                continue;
            }
            String home = normalizeFifaCode(FifaMatchParser.teamCode(match.get("Home")));
            String away = normalizeFifaCode(FifaMatchParser.teamCode(match.get("Away")));
            if (home == null || away == null) {
                continue;
            }
            Integer homeScore = FifaMatchParser.homeScore(match);
            Integer awayScore = FifaMatchParser.awayScore(match);
            int homeGoals = homeScore != null ? homeScore : 0;
            int awayGoals = awayScore != null ? awayScore : 0;
            result.put(home, homeGoals + ":" + awayGoals);
            result.put(away, awayGoals + ":" + homeGoals);
        }
        return result;
    }

    static boolean isLive(Wc26ScheduleMatchDto match) {
        if (match.isFinalized()) {
            return false;
        }
        String status = MatchStatuses.normalize(match.getStatus());
        if (status != null && MatchStatuses.LIVE.contains(status)) {
            return true;
        }
        return match.getLiveMinuteLabel() != null && !match.getLiveMinuteLabel().isBlank();
    }

    static String compactScoreLine(String scoreView) {
        if (scoreView == null || scoreView.isBlank() || "—".equals(scoreView.trim())) {
            return null;
        }
        String trimmed = scoreView.trim();
        int space = trimmed.indexOf(' ');
        if (space > 0) {
            return trimmed.substring(0, space);
        }
        return trimmed;
    }

    static String scoreOrDefault(String scoreView) {
        String compact = compactScoreLine(scoreView);
        return compact != null ? compact : DEFAULT_LIVE_SCORE;
    }

    /** [homeGoals, awayGoals] */
    static int[] parseMatchGoals(String scoreView) {
        String compact = scoreOrDefault(scoreView);
        String[] parts = compact.split(":");
        int home = parts.length > 0 ? parseGoal(parts[0]) : 0;
        int away = parts.length > 1 ? parseGoal(parts[1]) : 0;
        return new int[] { home, away };
    }

    private static int parseGoal(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalizeFifaCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
