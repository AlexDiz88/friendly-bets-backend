package net.friendly_bets.tournamentarchive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.fifa.FifaMatchParser;
import net.friendly_bets.fifa.FifaStandingParser;
import net.friendly_bets.fifa.client.FifaHttpClient;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveBestThirdRow;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveBracketPair;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveMatch;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveStandingRow;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TournamentArchiveFifaExportService {

    private static final List<String> GROUP_LETTERS = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
    );
    private static final String DEFAULT_EDITION = "WC_2026";
    private static final Pattern WINNER = Pattern.compile("^W(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RUNNER_UP = Pattern.compile("^RU(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final FifaHttpClient fifaHttpClient;
    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;
    private final ObjectMapper objectMapper;

    public TournamentArchive exportAndWriteFile(String editionCode) {
        TournamentArchive archive = exportFromFifa(editionCode);
        Path path = reviewJsonPath(archive.getEditionCode());
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), archive);
        } catch (IOException e) {
            throw new BadRequestException("tournamentArchiveExportWriteError");
        }
        return archive;
    }

    public Path reviewJsonPath(String editionCode) {
        String code = normalizeEdition(editionCode);
        String fileName = "tournament-archive-" + code.toLowerCase(Locale.ROOT).replace('_', '-') + ".json";
        return Paths.get("data", fileName);
    }

    public TournamentArchive exportFromFifa(String editionCode) {
        String code = normalizeEdition(editionCode);
        if (!DEFAULT_EDITION.equals(code)) {
            throw new BadRequestException("tournamentArchiveEditionUnsupported");
        }

        List<JsonNode> fifaMatches;
        JsonNode standingsRoot;
        try {
            fifaMatches = fifaHttpClient.fetchAllCalendarMatches();
            standingsRoot = fifaHttpClient.fetchGroupStageStandings();
        } catch (Exception e) {
            throw new BadRequestException("tournamentArchiveFifaLoadError");
        }
        if (fifaMatches == null || fifaMatches.isEmpty()
                || standingsRoot == null || standingsRoot.get("Results") == null) {
            throw new BadRequestException("tournamentArchiveFifaLoadError");
        }

        Map<String, Team> teamByFifa = new HashMap<>();
        Set<String> unresolved = new LinkedHashSet<>();

        List<TournamentArchiveMatch> matches = new ArrayList<>();
        List<TournamentArchiveBracketPair> bracket = new ArrayList<>();
        for (JsonNode node : fifaMatches) {
            matches.add(toMatch(node, teamByFifa, unresolved));
            TournamentArchiveBracketPair pair = toBracketPair(node);
            if (pair != null) {
                bracket.add(pair);
            }
        }
        matches.sort(Comparator.comparingInt(TournamentArchiveMatch::getMatchNumber));
        bracket.sort(Comparator.comparingInt(TournamentArchiveBracketPair::getMatchNumber));

        List<TournamentArchiveStandingRow> groupStandings = buildGroupStandings(
                standingsRoot, teamByFifa, unresolved);
        List<TournamentArchiveBestThirdRow> bestThirds = buildBestThirdPlaces(groupStandings);
        applyQualificationStatus(groupStandings, bestThirds);

        return TournamentArchive.builder()
                .editionCode(code)
                .competitionType("WC")
                .name("FIFA World Cup 2026")
                .year(2026)
                .source("fifa")
                .exportedAt(LocalDateTime.now())
                .matches(matches)
                .bracket(bracket)
                .groupStandings(groupStandings)
                .bestThirdPlaces(bestThirds)
                .unresolvedTeams(new ArrayList<>(unresolved))
                .build();
    }

    private TournamentArchiveMatch toMatch(
            JsonNode node,
            Map<String, Team> teamByFifa,
            Set<String> unresolved
    ) {
        String stage = resolveStage(node);
        String homeFifa = FifaMatchParser.teamCode(node.get("Home"));
        String awayFifa = FifaMatchParser.teamCode(node.get("Away"));
        Team home = resolveTeam(homeFifa, teamByFifa, unresolved);
        Team away = resolveTeam(awayFifa, teamByFifa, unresolved);
        String winnerFifa = FifaMatchParser.winnerCode(node);
        Team winner = resolveTeam(winnerFifa, teamByFifa, unresolved);

        TournamentArchiveMatch match = TournamentArchiveMatch.builder()
                .matchNumber(FifaMatchParser.matchNumber(node))
                .stage(stage)
                .group(FifaMatchParser.isGroupStage(node) ? FifaMatchParser.groupLetter(node) : null)
                .kickoffUtc(FifaMatchParser.utcDate(node))
                .homeTeamId(home != null ? home.getId() : null)
                .awayTeamId(away != null ? away.getId() : null)
                .winnerTeamId(winner != null ? winner.getId() : null)
                .homeTeamFifaCode(homeFifa)
                .awayTeamFifaCode(awayFifa)
                .winnerTeamFifaCode(winnerFifa)
                .build();

        TournamentArchiveScoreMapper.applyScore(match, node);
        return match;
    }

    private static TournamentArchiveBracketPair toBracketPair(JsonNode node) {
        if (FifaMatchParser.isGroupStage(node)) {
            return null;
        }
        Integer home = matchNumberFromPlaceholder(FifaMatchParser.placeholderHome(node));
        Integer away = matchNumberFromPlaceholder(FifaMatchParser.placeholderAway(node));
        if (home == null && away == null) {
            return null;
        }
        String from = isRunnerUpPlaceholder(FifaMatchParser.placeholderHome(node))
                || isRunnerUpPlaceholder(FifaMatchParser.placeholderAway(node))
                ? "runner_up"
                : "winner";
        return TournamentArchiveBracketPair.builder()
                .matchNumber(FifaMatchParser.matchNumber(node))
                .home(home)
                .away(away)
                .from(from)
                .build();
    }

    private static Integer matchNumberFromPlaceholder(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher winner = WINNER.matcher(raw.trim());
        if (winner.matches()) {
            return Integer.parseInt(winner.group(1));
        }
        Matcher runnerUp = RUNNER_UP.matcher(raw.trim());
        if (runnerUp.matches()) {
            return Integer.parseInt(runnerUp.group(1));
        }
        return null;
    }

    private static boolean isRunnerUpPlaceholder(String raw) {
        return raw != null && RUNNER_UP.matcher(raw.trim()).matches();
    }

    private static String resolveStage(JsonNode node) {
        if (FifaMatchParser.isGroupStage(node)) {
            return TournamentArchiveStages.groupStageForMatchNumber(FifaMatchParser.matchNumber(node));
        }
        String mapped = FifaMatchParser.mapKnockoutStage(FifaMatchParser.stageDescription(node));
        if (mapped == null) {
            return null;
        }
        return switch (mapped) {
            case "round_of_32" -> "1/16";
            case "round_of_16" -> "1/8";
            case "quarter_final" -> "1/4";
            case "semi_final" -> "1/2";
            default -> mapped;
        };
    }

    private Team resolveTeam(String fifaCode, Map<String, Team> cache, Set<String> unresolved) {
        if (fifaCode == null || fifaCode.isBlank()) {
            return null;
        }
        String code = fifaCode.trim().toUpperCase(Locale.ROOT);
        if (cache.containsKey(code)) {
            return cache.get(code);
        }
        Optional<Team> resolved = teamAliasResolver.resolveWc26Code(code)
                .or(() -> teamsRepository.findByCountryIgnoreCase(code));
        if (resolved.isPresent()) {
            cache.put(code, resolved.get());
            return resolved.get();
        }
        unresolved.add(code);
        cache.put(code, null);
        return null;
    }

    private List<TournamentArchiveStandingRow> buildGroupStandings(
            JsonNode standingsRoot,
            Map<String, Team> teamByFifa,
            Set<String> unresolved
    ) {
        JsonNode results = standingsRoot.get("Results");
        Map<String, List<JsonNode>> byGroup = new LinkedHashMap<>();
        for (String letter : GROUP_LETTERS) {
            byGroup.put(letter, new ArrayList<>());
        }
        for (JsonNode row : results) {
            String letter = FifaStandingParser.groupLetter(row);
            if (letter != null && byGroup.containsKey(letter)) {
                byGroup.get(letter).add(row);
            }
        }

        List<TournamentArchiveStandingRow> rows = new ArrayList<>();
        for (String letter : GROUP_LETTERS) {
            List<JsonNode> groupRows = new ArrayList<>(byGroup.get(letter));
            groupRows.sort(Comparator.comparingInt(FifaStandingParser::position));
            for (JsonNode row : groupRows) {
                String fifa = FifaStandingParser.teamCode(row);
                Team team = resolveTeam(fifa, teamByFifa, unresolved);
                rows.add(TournamentArchiveStandingRow.builder()
                        .group(letter)
                        .rank(FifaStandingParser.position(row))
                        .teamId(team != null ? team.getId() : null)
                        .fifaCode(fifa)
                        .played(FifaStandingParser.played(row))
                        .wins(FifaStandingParser.wins(row))
                        .draws(FifaStandingParser.draws(row))
                        .losses(FifaStandingParser.losses(row))
                        .goalsFor(FifaStandingParser.goalsFor(row))
                        .goalsAgainst(FifaStandingParser.goalsAgainst(row))
                        .goalDifference(FifaStandingParser.goalDifference(row))
                        .points(FifaStandingParser.points(row))
                        .qualificationStatus("pending")
                        .build());
            }
        }
        return rows;
    }

    private static List<TournamentArchiveBestThirdRow> buildBestThirdPlaces(
            List<TournamentArchiveStandingRow> groupStandings
    ) {
        Map<String, List<TournamentArchiveStandingRow>> byGroup = new LinkedHashMap<>();
        for (TournamentArchiveStandingRow row : groupStandings) {
            byGroup.computeIfAbsent(row.getGroup(), k -> new ArrayList<>()).add(row);
        }
        List<TournamentArchiveBestThirdRow> thirds = new ArrayList<>();
        for (String letter : GROUP_LETTERS) {
            List<TournamentArchiveStandingRow> rows = byGroup.getOrDefault(letter, List.of());
            if (rows.size() < 3) {
                continue;
            }
            TournamentArchiveStandingRow third = rows.stream()
                    .filter(r -> r.getRank() == 3)
                    .findFirst()
                    .orElse(rows.get(2));
            thirds.add(TournamentArchiveBestThirdRow.builder()
                    .group(letter)
                    .teamId(third.getTeamId())
                    .fifaCode(third.getFifaCode())
                    .played(third.getPlayed())
                    .wins(third.getWins())
                    .draws(third.getDraws())
                    .losses(third.getLosses())
                    .points(third.getPoints())
                    .goalDifference(third.getGoalDifference())
                    .goalsFor(third.getGoalsFor())
                    .goalsAgainst(third.getGoalsAgainst())
                    .qualifies(false)
                    .build());
        }
        thirds.sort(TournamentArchiveFifaExportService::compareBestThird);
        for (int i = 0; i < thirds.size(); i++) {
            thirds.get(i).setRank(i + 1);
            thirds.get(i).setQualifies(i < 8);
        }
        return thirds;
    }

    private static void applyQualificationStatus(
            List<TournamentArchiveStandingRow> groupStandings,
            List<TournamentArchiveBestThirdRow> bestThirdPlaces
    ) {
        Set<String> qualifying = new LinkedHashSet<>();
        for (TournamentArchiveBestThirdRow third : bestThirdPlaces) {
            if (third.isQualifies()) {
                qualifying.add(third.getGroup() + "|" + third.getFifaCode());
            }
        }
        for (TournamentArchiveStandingRow row : groupStandings) {
            if (row.getRank() <= 2) {
                row.setQualificationStatus("direct");
            } else if (row.getRank() == 3
                    && qualifying.contains(row.getGroup() + "|" + row.getFifaCode())) {
                row.setQualificationStatus("best_third");
            } else {
                row.setQualificationStatus("eliminated");
            }
        }
    }

    private static int compareBestThird(TournamentArchiveBestThirdRow a, TournamentArchiveBestThirdRow b) {
        int byPoints = Integer.compare(b.getPoints(), a.getPoints());
        if (byPoints != 0) {
            return byPoints;
        }
        int byGd = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
        if (byGd != 0) {
            return byGd;
        }
        int byGf = Integer.compare(b.getGoalsFor(), a.getGoalsFor());
        if (byGf != 0) {
            return byGf;
        }
        return a.getGroup().compareTo(b.getGroup());
    }

    private static String normalizeEdition(String editionCode) {
        if (editionCode == null || editionCode.isBlank()) {
            return DEFAULT_EDITION;
        }
        return editionCode.trim().toUpperCase(Locale.ROOT);
    }
}
