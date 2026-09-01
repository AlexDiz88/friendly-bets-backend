package net.friendly_bets.eurofootball;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.live.LiveMatchSnapshot;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses euro-football.ru LIVE JSON ({@code GET /online/data}) — format reference / tests;
 * LIVE sync uses {@link EuroFootballDateHtmlParser} per kickoff date.
 */
@Component
public class EuroFootballDateJsonParser {

    private static final Pattern MINUTE_BEFORE_MIN = Pattern.compile("(\\d{1,3}(?:\\+\\d{1,2})?)\\s*мин");
    private static final Pattern MINUTE_IN_TEXT = Pattern.compile("(\\d{1,3}(?:\\+\\d{1,2})?)");

    private final ObjectMapper objectMapper;

    public EuroFootballDateJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EuroFootballParsedDatePage parse(String json) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("euroFootballFetchFailed");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode tournaments = root.get("tournaments");
            List<EuroFootballParsedDatePage.CompetitionBlock> blocks = new ArrayList<>();
            if (tournaments != null && tournaments.isArray()) {
                for (JsonNode tournament : tournaments) {
                    EuroFootballParsedDatePage.CompetitionBlock block = parseTournament(tournament);
                    if (block != null && !block.getMatches().isEmpty()) {
                        blocks.add(block);
                    }
                }
            }
            return EuroFootballParsedDatePage.builder().competitions(blocks).build();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("euroFootballFetchFailed");
        }
    }

    private static EuroFootballParsedDatePage.CompetitionBlock parseTournament(JsonNode tournament) {
        if (tournament == null || !tournament.isObject()) {
            return null;
        }
        Integer tournamentId = tournament.hasNonNull("turnir_id") ? tournament.get("turnir_id").asInt() : null;
        String slug = text(tournament, "slug");
        String parentSlug = text(tournament, "parent_slug");
        String title = displayTitle(text(tournament, "pre_title"), text(tournament, "title"));
        JsonNode matchesNode = tournament.get("matches");
        List<EuroFootballParsedDatePage.MatchRow> rows = new ArrayList<>();
        if (matchesNode != null && matchesNode.isArray()) {
            for (JsonNode match : matchesNode) {
                EuroFootballParsedDatePage.MatchRow row = parseMatch(match);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return EuroFootballParsedDatePage.CompetitionBlock.builder()
                .tournamentId(tournamentId)
                .title(title)
                .slug(slug)
                .parentSlug(parentSlug)
                .matches(rows)
                .build();
    }

    private static EuroFootballParsedDatePage.MatchRow parseMatch(JsonNode match) {
        if (match == null || !match.isObject()) {
            return null;
        }
        String home = text(match, "team1");
        String away = text(match, "team2");
        if (home == null || away == null) {
            return null;
        }
        String statusRaw = text(match, "status");
        int statusCode = match.hasNonNull("statusCode") ? match.get("statusCode").asInt() : -1;
        String statusTextHtml = text(match, "statusText");
        String statusTextPlain = plainStatusText(statusTextHtml);
        String mappedStatus = mapStatus(statusRaw, statusCode, statusTextPlain);
        String rawMinute = extractMinuteLabel(mappedStatus, statusTextPlain);
        String fullTime = scoreAllowed(mappedStatus) ? formatScore(match.get("goals1"), match.get("goals2")) : null;
        String scoreText = fullTime == null ? null : fullTime.replace(":", " : ");

        String externalId = match.hasNonNull("id") ? match.get("id").asText() : null;
        return EuroFootballParsedDatePage.MatchRow.builder()
                .externalMatchId(externalId)
                .homeName(home)
                .awayName(away)
                .scoreText(scoreText)
                .snapshot(new LiveMatchSnapshot(mappedStatus, rawMinute, fullTime, null))
                .build();
    }

    /**
     * euro-football {@code status} / {@code statusCode} / plain {@code statusText} → canonical status.
     * Observed: soon=0, live 1H=1, HT=2, 2H=3, finished=9, finished after pens=16.
     */
    static String mapStatus(String status, int statusCode, String statusTextPlain) {
        String st = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        String text = statusTextPlain == null ? "" : statusTextPlain.toLowerCase(Locale.ROOT);
        if (text.contains("отмен")) {
            return "CANCELED";
        }
        if ("finished".equals(st) || text.contains("окончен")) {
            return "FINISHED";
        }
        if ("soon".equals(st)) {
            return "SCHEDULED";
        }
        if ("live".equals(st)) {
            if (statusCode == 2 || text.contains("перерыв")) {
                return "PAUSED";
            }
            if (text.contains("пен")) {
                return "PENALTY_SHOOTOUT";
            }
            if (text.contains("доп")) {
                return "EXTRA_TIME";
            }
            return "IN_PLAY";
        }
        return "SCHEDULED";
    }

    static boolean scoreAllowed(String mappedStatus) {
        return "IN_PLAY".equals(mappedStatus)
                || "PAUSED".equals(mappedStatus)
                || "EXTRA_TIME".equals(mappedStatus)
                || "PENALTY_SHOOTOUT".equals(mappedStatus)
                || "FINISHED".equals(mappedStatus);
    }

    static String extractMinuteLabel(String mappedStatus, String statusTextPlain) {
        if (!"IN_PLAY".equals(mappedStatus) && !"EXTRA_TIME".equals(mappedStatus)) {
            return null;
        }
        if (statusTextPlain == null || statusTextPlain.isBlank()) {
            return null;
        }
        Matcher beforeMin = MINUTE_BEFORE_MIN.matcher(statusTextPlain);
        if (beforeMin.find()) {
            return beforeMin.group(1);
        }
        Matcher matcher = MINUTE_IN_TEXT.matcher(statusTextPlain);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    static String plainStatusText(String statusTextHtml) {
        if (statusTextHtml == null || statusTextHtml.isBlank()) {
            return null;
        }
        String trimmed = statusTextHtml.trim();
        if (!trimmed.contains("<")) {
            return trimmed;
        }
        String text = Jsoup.parse(trimmed).text();
        return text == null || text.isBlank() ? null : text.trim();
    }

    static String displayTitle(String preTitle, String title) {
        if (preTitle == null || preTitle.isBlank()) {
            return title;
        }
        if (title == null || title.isBlank()) {
            return preTitle;
        }
        return preTitle + " " + title;
    }

    private static String formatScore(JsonNode goals1, JsonNode goals2) {
        if (goals1 == null || goals2 == null
                || goals1.isMissingNode() || goals2.isMissingNode()
                || goals1.isNull() || goals2.isNull()
                || !goals1.isNumber() || !goals2.isNumber()) {
            return null;
        }
        return goals1.asInt() + ":" + goals2.asInt();
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
