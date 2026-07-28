package net.friendly_bets.football24;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses football24 {@code /fixture/getFixturesRounds} JSON.
 * Kickoff only from {@code startingAt} ISO-8601 with {@code Z} (or offset) → Instant UTC.
 * Numeric tours only ({@code ТУР N} / {@code Тур N}); qualification rounds are ignored.
 */
@Component
@RequiredArgsConstructor
public class Football24ScheduleParser {

    private static final Pattern TOUR_NUMBER = Pattern.compile(
            "(?iu)^\\s*тур\\s+(\\d+)\\s*$"
    );
    private static final Pattern QUALIFYING = Pattern.compile(
            "(?iu)кваліфікац|квалификац|отбороч|qualif"
    );

    private final ObjectMapper objectMapper;

    public Football24ParsedSchedule parseFixturesRounds(String json, int seasonId) {
        List<Football24ParsedSchedule.Round> rounds = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
            }
            for (JsonNode roundNode : data) {
                String rawName = text(roundNode.path("round"));
                if (isQualifyingRound(rawName)) {
                    continue;
                }
                OptionalInt tourNo = parseTourNumber(rawName);
                if (tourNo.isEmpty()) {
                    continue;
                }
                Football24ParsedSchedule.Round round = Football24ParsedSchedule.Round.builder()
                        .number(tourNo.getAsInt())
                        .rawName(rawName)
                        .matches(new ArrayList<>())
                        .build();
                JsonNode fixtures = roundNode.path("fixtures");
                if (fixtures.isArray()) {
                    for (JsonNode fx : fixtures) {
                        parseMatch(fx).ifPresent(round.getMatches()::add);
                    }
                }
                rounds.add(round);
            }
        } catch (Exception ignored) {
            return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
        }
        return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
    }

    /**
     * Resolves football24 season id from {@code /season/getSeasonsByLeagueId} JSON.
     * Prefers season whose name starts with {@code externalSeasonYear} (e.g. 2026 → 2026/2027).
     */
    public OptionalInt resolveSeasonId(String seasonsJson, int externalSeasonYear) {
        if (seasonsJson == null || seasonsJson.isBlank() || externalSeasonYear < 1990) {
            return OptionalInt.empty();
        }
        String yearPrefix = String.valueOf(externalSeasonYear);
        try {
            JsonNode data = objectMapper.readTree(seasonsJson).path("data");
            if (!data.isArray()) {
                return OptionalInt.empty();
            }
            OptionalInt byYear = OptionalInt.empty();
            OptionalInt currentFallback = OptionalInt.empty();
            for (JsonNode season : data) {
                int id = season.path("id").asInt(0);
                if (id <= 0) {
                    continue;
                }
                String name = text(season.path("name"));
                if (name.startsWith(yearPrefix)) {
                    byYear = OptionalInt.of(id);
                    break;
                }
                if (currentFallback.isEmpty() && season.path("isCurrent").asBoolean(false)) {
                    currentFallback = OptionalInt.of(id);
                }
            }
            if (byYear.isPresent()) {
                return byYear;
            }
            return currentFallback;
        } catch (Exception e) {
            return OptionalInt.empty();
        }
    }

    public List<String> parseTeamNamesFromMatchday(String fixturesJson, int seasonId, int matchday) {
        Football24ParsedSchedule parsed = parseFixturesRounds(fixturesJson, seasonId);
        Football24ParsedSchedule.Round round = parsed.roundsByNumber().get(matchday);
        if (round == null || round.getMatches() == null || round.getMatches().isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (Football24ParsedSchedule.Match match : round.getMatches()) {
            if (match.getHomeName() != null && !match.getHomeName().isBlank()) {
                names.add(match.getHomeName().trim());
            }
            if (match.getAwayName() != null && !match.getAwayName().isBlank()) {
                names.add(match.getAwayName().trim());
            }
        }
        return new ArrayList<>(names);
    }

    static OptionalInt parseTourNumber(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = TOUR_NUMBER.matcher(rawName.trim());
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    static boolean isQualifyingRound(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return false;
        }
        return QUALIFYING.matcher(rawName).find();
    }

    private Optional<Football24ParsedSchedule.Match> parseMatch(JsonNode fx) {
        String homeName = text(fx.path("teamHome").path("name"));
        String awayName = text(fx.path("teamAway").path("name"));
        if (homeName.isBlank() || awayName.isBlank()) {
            return Optional.empty();
        }
        Instant utcKickoff = parseStartingAt(text(fx.path("startingAt")));
        boolean hasScore = !fx.path("score").isNull()
                && !fx.path("score").isMissingNode()
                && !text(fx.path("score")).isBlank();
        boolean hasHomeGoals = !fx.path("teamHomeScore").isNull()
                && !fx.path("teamHomeScore").isMissingNode();
        boolean hasAwayGoals = !fx.path("teamAwayScore").isNull()
                && !fx.path("teamAwayScore").isMissingNode();
        String status = (hasScore || hasHomeGoals || hasAwayGoals) ? "FINISHED" : "SCHEDULED";
        return Optional.of(Football24ParsedSchedule.Match.builder()
                .homeName(homeName)
                .awayName(awayName)
                .utcKickoff(utcKickoff)
                .status(status)
                .build());
    }

    /**
     * Accepts only ISO-8601 with Z or numeric offset (e.g. {@code 2026-08-21T19:00:00.000Z}).
     */
    public Instant parseStartingAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        String v = node.asText(null);
        return v == null ? "" : v.trim();
    }
}
