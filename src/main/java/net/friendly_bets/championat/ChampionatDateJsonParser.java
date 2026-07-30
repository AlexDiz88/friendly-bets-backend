package net.friendly_bets.championat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.live.LiveMatchSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses championat.com match-center JSON ({@code /stat/data/{date}/football}).
 * Status comes from {@code status.label} — no minute-based period heuristics.
 */
@Component
public class ChampionatDateJsonParser {

    /** Minute always ends with {@code '} on championat (e.g. {@code 2-й тайм, 75'}). */
    private static final Pattern MINUTE_IN_STATUS = Pattern.compile(
            "(\\d{1,3}(?:\\+\\d{1,2})?)\\s*'"
    );

    private final ObjectMapper objectMapper;

    public ChampionatDateJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChampionatParsedDatePage parse(String json) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("championatFetchFailed");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode tournaments = root.path("matches").path("football").path("tournaments");
            List<ChampionatParsedDatePage.CompetitionBlock> blocks = new ArrayList<>();
            if (tournaments.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = tournaments.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    ChampionatParsedDatePage.CompetitionBlock block = parseTournament(entry.getValue());
                    if (block != null && !block.getMatches().isEmpty()) {
                        blocks.add(block);
                    }
                }
            }
            return ChampionatParsedDatePage.builder().competitions(blocks).build();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("championatFetchFailed");
        }
    }

    private static ChampionatParsedDatePage.CompetitionBlock parseTournament(JsonNode tournament) {
        if (tournament == null || !tournament.isObject()) {
            return null;
        }
        Integer tournamentId = tournament.hasNonNull("id") ? tournament.get("id").asInt() : null;
        String title = text(tournament, "name");
        JsonNode matchesNode = tournament.get("matches");
        List<ChampionatParsedDatePage.MatchRow> rows = new ArrayList<>();
        if (matchesNode != null && matchesNode.isObject()) {
            Iterator<JsonNode> it = matchesNode.elements();
            while (it.hasNext()) {
                ChampionatParsedDatePage.MatchRow row = parseMatch(it.next());
                if (row != null) {
                    rows.add(row);
                }
            }
        } else if (matchesNode != null && matchesNode.isArray()) {
            for (JsonNode match : matchesNode) {
                ChampionatParsedDatePage.MatchRow row = parseMatch(match);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return ChampionatParsedDatePage.CompetitionBlock.builder()
                .tournamentId(tournamentId)
                .title(title)
                .matches(rows)
                .build();
    }

    private static ChampionatParsedDatePage.MatchRow parseMatch(JsonNode match) {
        if (match == null || !match.isObject()) {
            return null;
        }
        JsonNode teams = match.get("teams");
        if (teams == null || !teams.isArray() || teams.size() < 2) {
            return null;
        }
        String home = text(teams.get(0), "name");
        String away = text(teams.get(1), "name");
        if (home == null || away == null) {
            return null;
        }
        String statusLabel = text(match.path("status"), "label");
        String statusName = text(match.path("status"), "name");
        String mappedStatus = mapStatus(statusLabel);
        String rawMinute = extractMinuteLabel(statusLabel, statusName);
        String fullTime = formatScore(match.path("score"));
        String penalty = formatShootoutScore(match.path("score"));
        String scoreText = buildScoreText(fullTime, match.path("score").path("suffix").asText(null));

        String externalId = match.hasNonNull("id") ? match.get("id").asText() : null;
        return ChampionatParsedDatePage.MatchRow.builder()
                .externalMatchId(externalId)
                .homeName(home)
                .awayName(away)
                .scoreText(scoreText)
                .snapshot(new LiveMatchSnapshot(mappedStatus, rawMinute, fullTime, null, penalty))
                .build();
    }

    /**
     * championat {@code status.label} → canonical match_schedules status.
     */
    static String mapStatus(String label) {
        if (label == null || label.isBlank()) {
            return "SCHEDULED";
        }
        return switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "1t", "2t" -> "IN_PLAY";
            case "half" -> "PAUSED";
            case "extra" -> "EXTRA_TIME";
            case "pen" -> "PENALTY_SHOOTOUT";
            case "fin" -> "FINISHED";
            case "dns" -> "SCHEDULED";
            case "cans" -> "CANCELED";
            default -> "SCHEDULED";
        };
    }

    static String extractMinuteLabel(String statusLabel, String statusName) {
        if (statusLabel == null) {
            return null;
        }
        String label = statusLabel.trim().toLowerCase(Locale.ROOT);
        if (!label.equals("1t") && !label.equals("2t") && !label.equals("extra")) {
            return null;
        }
        if (statusName == null || statusName.isBlank()) {
            return null;
        }
        Matcher matcher = MINUTE_IN_STATUS.matcher(statusName);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String formatScore(JsonNode score) {
        if (score == null || score.isMissingNode() || score.isNull()) {
            return null;
        }
        if (score.has("totalHome") && score.has("totalAway")
                && !score.get("totalHome").isNull() && !score.get("totalAway").isNull()) {
            return score.get("totalHome").asInt() + ":" + score.get("totalAway").asInt();
        }
        String main = text(score.path("direct"), "main");
        if (main != null) {
            return main.replace(" ", "").replace('：', ':');
        }
        return null;
    }

    /** Penalty shootout: {@code shootoutHome}/{@code shootoutAway} → {@code "2:3"}. */
    static String formatShootoutScore(JsonNode score) {
        if (score == null || score.isMissingNode() || score.isNull()) {
            return null;
        }
        if (score.has("shootoutHome") && score.has("shootoutAway")
                && !score.get("shootoutHome").isNull() && !score.get("shootoutAway").isNull()) {
            return score.get("shootoutHome").asInt() + ":" + score.get("shootoutAway").asInt();
        }
        return null;
    }

    private static String buildScoreText(String fullTime, String suffix) {
        if (fullTime == null) {
            return null;
        }
        if (suffix != null && !suffix.isBlank()) {
            return fullTime.replace(":", " : ") + " " + suffix.trim();
        }
        return fullTime.replace(":", " : ");
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
