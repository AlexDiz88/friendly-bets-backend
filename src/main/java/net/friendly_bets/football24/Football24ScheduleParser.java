package net.friendly_bets.football24;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses football24 {@code /fixture/getFixturesRounds} JSON.
 * Kickoff only from {@code startingAt} ISO-8601 with {@code Z} (or offset) → Instant UTC.
 * EPL/BL: numeric tours ({@code ТУР N}). CL/LE league phase: one bucket
 * ({@code Груповий етап} / similar) → split into matchdays by UTC kickoff gaps.
 * Qualification rounds are ignored.
 */
@Component
@RequiredArgsConstructor
public class Football24ScheduleParser {

    /** Max calendar-day gap (UTC) within one matchday cluster; larger gap starts the next tour. */
    static final int LEAGUE_PHASE_MATCHDAY_GAP_DAYS = 3;

    private static final Pattern TOUR_NUMBER = Pattern.compile(
            "(?iu)^\\s*тур\\s+(\\d+)\\s*$"
    );
    private static final Pattern QUALIFYING = Pattern.compile(
            "(?iu)кваліфікац|квалификац|отбороч|qualif"
    );
    /** UA/RU league-phase / main-stage labels used for CL/LE instead of {@code ТУР N}. */
    private static final Pattern LEAGUE_PHASE = Pattern.compile(
            "(?iu)групов\\w*\\s+етап|групп\\w*\\s+этап|основн\\w*\\s+етап|основн\\w*\\s+этап"
                    + "|ліга\\s+чемпіонів|лига\\s+чемпионов|ліга\\s+європи|лига\\s+европы"
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
                List<Football24ParsedSchedule.Match> fixtures = parseFixtures(roundNode.path("fixtures"));
                OptionalInt tourNo = parseTourNumber(rawName);
                if (tourNo.isPresent()) {
                    rounds.add(Football24ParsedSchedule.Round.builder()
                            .number(tourNo.getAsInt())
                            .rawName(rawName)
                            .matches(fixtures)
                            .build());
                    continue;
                }
                if (isLeaguePhaseRound(rawName)) {
                    rounds.addAll(expandLeaguePhaseIntoMatchdays(rawName, fixtures));
                }
            }
        } catch (Exception ignored) {
            return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
        }
        return Football24ParsedSchedule.builder().seasonId(seasonId).rounds(rounds).build();
    }

    private List<Football24ParsedSchedule.Match> parseFixtures(JsonNode fixtures) {
        List<Football24ParsedSchedule.Match> matches = new ArrayList<>();
        if (!fixtures.isArray()) {
            return matches;
        }
        for (JsonNode fx : fixtures) {
            parseMatch(fx).ifPresent(matches::add);
        }
        return matches;
    }

    /**
     * football24 CL/LE put the whole league phase under one label (no {@code ТУР N}).
     * Deduplicate identical fixtures, then split by UTC kickoff calendar gaps into matchdays 1..N.
     */
    static List<Football24ParsedSchedule.Round> expandLeaguePhaseIntoMatchdays(
            String rawName,
            List<Football24ParsedSchedule.Match> fixtures
    ) {
        List<Football24ParsedSchedule.Match> unique = dedupeLeaguePhaseMatches(fixtures);
        List<Football24ParsedSchedule.Match> withKickoff = new ArrayList<>();
        for (Football24ParsedSchedule.Match match : unique) {
            if (match.getUtcKickoff() != null) {
                withKickoff.add(match);
            }
        }
        withKickoff.sort(Comparator.comparing(Football24ParsedSchedule.Match::getUtcKickoff));

        List<Football24ParsedSchedule.Round> rounds = new ArrayList<>();
        if (withKickoff.isEmpty()) {
            return rounds;
        }

        List<Football24ParsedSchedule.Match> current = new ArrayList<>();
        LocalDate clusterLastDate = null;
        int matchday = 1;
        for (Football24ParsedSchedule.Match match : withKickoff) {
            LocalDate date = match.getUtcKickoff().atZone(ZoneOffset.UTC).toLocalDate();
            if (clusterLastDate != null
                    && ChronoUnit.DAYS.between(clusterLastDate, date) > LEAGUE_PHASE_MATCHDAY_GAP_DAYS) {
                rounds.add(Football24ParsedSchedule.Round.builder()
                        .number(matchday++)
                        .rawName(rawName)
                        .matches(current)
                        .build());
                current = new ArrayList<>();
            }
            current.add(match);
            clusterLastDate = date;
        }
        if (!current.isEmpty()) {
            rounds.add(Football24ParsedSchedule.Round.builder()
                    .number(matchday)
                    .rawName(rawName)
                    .matches(current)
                    .build());
        }
        return rounds;
    }

    /**
     * football24 may list the same fixture twice (different ids, same teams + kickoff).
     * Pair is unordered so home/away swap of the same event collapses.
     */
    static List<Football24ParsedSchedule.Match> dedupeLeaguePhaseMatches(
            List<Football24ParsedSchedule.Match> fixtures
    ) {
        if (fixtures == null || fixtures.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Football24ParsedSchedule.Match> out = new ArrayList<>();
        for (Football24ParsedSchedule.Match match : fixtures) {
            String key = leaguePhaseDedupeKey(match);
            if (!seen.add(key)) {
                continue;
            }
            out.add(match);
        }
        return out;
    }

    static String leaguePhaseDedupeKey(Football24ParsedSchedule.Match match) {
        String home = normalizeTeamKey(match.getHomeName());
        String away = normalizeTeamKey(match.getAwayName());
        String a = home.compareTo(away) <= 0 ? home : away;
        String b = home.compareTo(away) <= 0 ? away : home;
        String kickoff = match.getUtcKickoff() != null ? match.getUtcKickoff().toString() : "";
        return a + "|" + b + "|" + kickoff;
    }

    private static String normalizeTeamKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isLeaguePhaseRound(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return false;
        }
        return LEAGUE_PHASE.matcher(rawName.trim()).find();
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
        return teamNamesFromRound(round);
    }

    /**
     * Team aliases: all non-qualifying rounds with fixtures.
     * football24 may omit finished {@code ТУР 1}; CL/LE use league-phase round labels instead of {@code ТУР N}.
     */
    public List<String> parseTeamNamesFromFixturesRounds(String fixturesJson, int seasonId) {
        Set<String> names = new LinkedHashSet<>();
        if (fixturesJson == null || fixturesJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode data = objectMapper.readTree(fixturesJson).path("data");
            if (!data.isArray()) {
                return List.of();
            }
            for (JsonNode roundNode : data) {
                String rawName = text(roundNode.path("round"));
                if (isQualifyingRound(rawName)) {
                    continue;
                }
                JsonNode fixtures = roundNode.path("fixtures");
                if (!fixtures.isArray() || fixtures.isEmpty()) {
                    continue;
                }
                for (JsonNode fx : fixtures) {
                    parseMatch(fx).ifPresent(match -> {
                        addTeamName(names, match.getHomeName());
                        addTeamName(names, match.getAwayName());
                    });
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return new ArrayList<>(names);
    }

    private static List<String> teamNamesFromRound(Football24ParsedSchedule.Round round) {
        Set<String> names = new LinkedHashSet<>();
        for (Football24ParsedSchedule.Match match : round.getMatches()) {
            addTeamName(names, match.getHomeName());
            addTeamName(names, match.getAwayName());
        }
        return new ArrayList<>(names);
    }

    private static void addTeamName(Set<String> names, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        names.add(raw.trim());
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
