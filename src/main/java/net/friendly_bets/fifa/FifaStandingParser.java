package net.friendly_bets.fifa;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FifaStandingParser {

    private FifaStandingParser() {
    }

    public static String groupLetter(JsonNode row) {
        JsonNode groups = row.get("Group");
        if (groups == null || !groups.isArray() || groups.isEmpty()) {
            return null;
        }
        JsonNode descriptionNode = groups.get(0).get("Description");
        if (descriptionNode == null || descriptionNode.isNull()) {
            return null;
        }
        String description = descriptionNode.asText("").trim();
        if (description.isBlank()) {
            return null;
        }
        int space = description.lastIndexOf(' ');
        if (space >= 0 && space < description.length() - 1) {
            return description.substring(space + 1).toUpperCase(Locale.ROOT);
        }
        return description.toUpperCase(Locale.ROOT);
    }

    public static String teamCode(JsonNode row) {
        JsonNode team = row.get("Team");
        if (team != null && !team.isNull()) {
            String fromTeam = FifaMatchParser.teamCode(team);
            if (fromTeam != null) {
                return fromTeam;
            }
        }
        JsonNode idTeam = row.get("IdTeam");
        return idTeam != null && !idTeam.isNull() ? idTeam.asText().trim().toUpperCase(Locale.ROOT) : null;
    }

    public static int position(JsonNode row) {
        return intOrZero(row.get("Position"));
    }

    public static int played(JsonNode row) {
        return intOrZero(row.get("Played"));
    }

    public static int wins(JsonNode row) {
        return intOrZero(row.get("Won"));
    }

    public static int draws(JsonNode row) {
        return intOrZero(row.get("Drawn"));
    }

    public static int losses(JsonNode row) {
        return intOrZero(row.get("Lost"));
    }

    public static int goalsFor(JsonNode row) {
        return intOrZero(row.get("For"));
    }

    public static int goalsAgainst(JsonNode row) {
        return intOrZero(row.get("Against"));
    }

    public static int goalDifference(JsonNode row) {
        JsonNode gd = row.get("GoalsDiference");
        if (gd != null && !gd.isNull()) {
            return gd.asInt();
        }
        return goalsFor(row) - goalsAgainst(row);
    }

    public static int points(JsonNode row) {
        return intOrZero(row.get("Points"));
    }

    public static boolean liveNow(JsonNode row) {
        JsonNode live = row.get("IsLive");
        return live != null && live.asBoolean(false);
    }

    /** Goals scored by this team in the in-progress match from {@code MatchResults}, or null if unknown. */
    public static Integer liveMatchGoalsFor(JsonNode row) {
        JsonNode match = findLiveMatch(row);
        if (match == null) {
            return null;
        }
        String teamId = teamId(row);
        if (teamId == null) {
            return null;
        }
        return teamGoalsInMatch(match, teamId);
    }

    /** Full score of the in-progress match (home:away), or null if unknown. */
    public static String liveMatchScore(JsonNode row) {
        JsonNode match = findLiveMatch(row);
        if (match == null) {
            return null;
        }
        Integer home = intOrNull(match.get("HomeTeamScore"));
        Integer away = intOrNull(match.get("AwayTeamScore"));
        if (home == null || away == null) {
            return null;
        }
        return home + ":" + away;
    }

    private static String teamId(JsonNode row) {
        JsonNode team = row.get("Team");
        if (team != null && !team.isNull()) {
            return textOrNull(team.get("IdTeam"));
        }
        return textOrNull(row.get("IdTeam"));
    }

    private static JsonNode findLiveMatch(JsonNode row) {
        if (!liveNow(row)) {
            return null;
        }
        String teamId = teamId(row);
        if (teamId == null) {
            return null;
        }
        JsonNode matchResults = row.get("MatchResults");
        if (matchResults == null || !matchResults.isArray()) {
            return null;
        }
        for (JsonNode match : matchResults) {
            int status = match.has("MatchStatus") ? match.get("MatchStatus").asInt(-1) : -1;
            if (status != 3 && status != 4) {
                continue;
            }
            if (teamGoalsInMatch(match, teamId) != null) {
                return match;
            }
        }
        for (JsonNode match : matchResults) {
            int status = match.has("MatchStatus") ? match.get("MatchStatus").asInt(-1) : -1;
            if (status == 0) {
                continue;
            }
            if (teamGoalsInMatch(match, teamId) != null) {
                return match;
            }
        }
        return null;
    }

    private static Integer teamGoalsInMatch(JsonNode match, String teamId) {
        String homeId = textOrNull(match.get("HomeTeamId"));
        String awayId = textOrNull(match.get("AwayTeamId"));
        if (teamId.equals(homeId)) {
            return intOrNull(match.get("HomeTeamScore"));
        }
        if (teamId.equals(awayId)) {
            return intOrNull(match.get("AwayTeamScore"));
        }
        return null;
    }

    private static Integer intOrNull(JsonNode node) {
        return node != null && node.isNumber() ? node.asInt() : null;
    }

    public static List<String> recentForm(JsonNode row) {
        JsonNode team = row.get("Team");
        String teamId = team != null && !team.isNull() ? textOrNull(team.get("IdTeam")) : textOrNull(row.get("IdTeam"));
        if (teamId == null) {
            return List.of();
        }
        JsonNode matchResults = row.get("MatchResults");
        if (matchResults == null || !matchResults.isArray()) {
            return List.of();
        }
        List<JsonNode> finished = new ArrayList<>();
        for (JsonNode match : matchResults) {
            if (match.get("HomeTeamScore") == null || match.get("HomeTeamScore").isNull()
                    || match.get("AwayTeamScore") == null || match.get("AwayTeamScore").isNull()) {
                continue;
            }
            finished.add(match);
        }
        finished.sort(Comparator.comparing(m -> textOrNull(m.get("StartTime")), Comparator.nullsLast(String::compareTo)));
        List<String> form = new ArrayList<>();
        for (JsonNode match : finished) {
            form.add(formLetter(match, teamId));
        }
        if (form.size() <= 3) {
            return form;
        }
        return form.subList(form.size() - 3, form.size());
    }

    private static String formLetter(JsonNode match, String teamId) {
        String homeId = textOrNull(match.get("HomeTeamId"));
        String awayId = textOrNull(match.get("AwayTeamId"));
        int homeScore = match.get("HomeTeamScore").asInt();
        int awayScore = match.get("AwayTeamScore").asInt();
        if (teamId.equals(homeId)) {
            if (homeScore > awayScore) {
                return "W";
            }
            if (homeScore < awayScore) {
                return "L";
            }
            return "D";
        }
        if (teamId.equals(awayId)) {
            if (awayScore > homeScore) {
                return "W";
            }
            if (awayScore < homeScore) {
                return "L";
            }
            return "D";
        }
        return "D";
    }

    private static int intOrZero(JsonNode node) {
        return node != null && node.isNumber() ? node.asInt() : 0;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText("").trim();
        return text.isEmpty() ? null : text;
    }
}
