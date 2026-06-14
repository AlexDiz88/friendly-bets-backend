package net.friendly_bets.fifa;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FifaMatchParser {

    private static final DateTimeFormatter FIFA_UTC = DateTimeFormatter.ISO_DATE_TIME;

    private FifaMatchParser() {
    }

    public static String stageDescription(JsonNode match) {
        return localized(match, "StageName");
    }

    public static String groupLetter(JsonNode match) {
        String groupName = localized(match, "GroupName");
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        String trimmed = groupName.trim();
        int space = trimmed.lastIndexOf(' ');
        if (space >= 0 && space < trimmed.length() - 1) {
            return trimmed.substring(space + 1).toUpperCase(Locale.ROOT);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static int matchNumber(JsonNode match) {
        JsonNode node = match.get("MatchNumber");
        return node != null && node.isNumber() ? node.asInt() : 0;
    }

    public static int matchStatus(JsonNode match) {
        JsonNode node = match.get("MatchStatus");
        return node != null && node.isNumber() ? node.asInt() : -1;
    }

    public static String mappedStatus(JsonNode match) {
        return switch (matchStatus(match)) {
            case 0 -> "FINISHED";
            case 3 -> "IN_PLAY";
            case 4 -> "PAUSED";
            default -> "SCHEDULED";
        };
    }

    public static String liveMinuteLabel(JsonNode match) {
        if (matchStatus(match) != 3) {
            return null;
        }
        JsonNode node = match.get("MatchTime");
        if (node == null || node.isNull() || node.asText("").isBlank()) {
            return null;
        }
        String raw = node.asText().trim();
        return raw.endsWith("'") ? raw : raw + "'";
    }

    public static String teamCode(JsonNode side) {
        if (side == null || side.isNull()) {
            return null;
        }
        JsonNode abbr = side.get("Abbreviation");
        if (abbr != null && !abbr.isNull() && !abbr.asText("").isBlank()) {
            return abbr.asText().trim().toUpperCase(Locale.ROOT);
        }
        JsonNode country = side.get("IdCountry");
        if (country != null && !country.isNull() && !country.asText("").isBlank()) {
            return country.asText().trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    static Integer teamScore(JsonNode side) {
        if (side == null || side.isNull()) {
            return null;
        }
        JsonNode score = side.get("Score");
        if (score != null && score.isNumber()) {
            return score.asInt();
        }
        return null;
    }

    public static Integer homeScore(JsonNode match) {
        Integer fromSide = teamScore(match.get("Home"));
        if (fromSide != null) {
            return fromSide;
        }
        JsonNode node = match.get("HomeTeamScore");
        return node != null && node.isNumber() ? node.asInt() : null;
    }

    public static Integer awayScore(JsonNode match) {
        Integer fromSide = teamScore(match.get("Away"));
        if (fromSide != null) {
            return fromSide;
        }
        JsonNode node = match.get("AwayTeamScore");
        return node != null && node.isNumber() ? node.asInt() : null;
    }

    public static String winnerCode(JsonNode match) {
        JsonNode winner = match.get("Winner");
        if (winner == null || winner.isNull()) {
            return null;
        }
        String winnerId = winner.asText("");
        JsonNode home = match.get("Home");
        JsonNode away = match.get("Away");
        if (home != null && winnerId.equals(home.path("IdTeam").asText())) {
            return teamCode(home);
        }
        if (away != null && winnerId.equals(away.path("IdTeam").asText())) {
            return teamCode(away);
        }
        return null;
    }

    public static String placeholderHome(JsonNode match) {
        return textOrNull(match.get("PlaceHolderA"));
    }

    public static String placeholderAway(JsonNode match) {
        return textOrNull(match.get("PlaceHolderB"));
    }

    public static LocalDateTime utcDate(JsonNode match) {
        JsonNode node = match.get("Date");
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return LocalDateTime.parse(node.asText(), FIFA_UTC);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isGroupStage(JsonNode match) {
        return "First Stage".equalsIgnoreCase(stageDescription(match));
    }

    public static boolean isFinished(JsonNode match) {
        return matchStatus(match) == 0;
    }

    public static boolean isLive(JsonNode match) {
        int status = matchStatus(match);
        return status == 3 || status == 4;
    }

    public static boolean countsForStandings(JsonNode match) {
        return isFinished(match);
    }

    public static String mapKnockoutStage(String stageDescription) {
        if (stageDescription == null) {
            return null;
        }
        return switch (stageDescription.trim().toLowerCase(Locale.ROOT)) {
            case "round of 32" -> "round_of_32";
            case "round of 16" -> "round_of_16";
            case "quarter-final", "quarter final" -> "quarter_final";
            case "semi-final", "semi final" -> "semi_final";
            case "play-off for third place", "playoff for third place" -> "third_place";
            case "final" -> "final";
            default -> null;
        };
    }

    private static String localized(JsonNode match, String field) {
        JsonNode array = match.get(field);
        if (array == null || !array.isArray() || array.isEmpty()) {
            return null;
        }
        for (JsonNode item : array) {
            JsonNode locale = item.get("Locale");
            if (locale != null && "en-GB".equalsIgnoreCase(locale.asText())) {
                JsonNode desc = item.get("Description");
                if (desc != null && !desc.isNull()) {
                    return desc.asText();
                }
            }
        }
        JsonNode desc = array.get(0).get("Description");
        return desc != null && !desc.isNull() ? desc.asText() : null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText("").trim();
        return text.isEmpty() ? null : text;
    }
}
