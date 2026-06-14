package net.friendly_bets.wc26;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.dto.Wc26FifaBestThirdRowDto;
import net.friendly_bets.dto.Wc26FifaBracketMatchDto;
import net.friendly_bets.dto.Wc26FifaBracketPageDto;
import net.friendly_bets.dto.Wc26FifaGroupTableDto;
import net.friendly_bets.dto.Wc26FifaStandingRowDto;
import net.friendly_bets.dto.Wc26FifaStandingsPageDto;
import net.friendly_bets.fifa.FifaMatchParser;
import net.friendly_bets.fifa.FifaStandingParser;
import net.friendly_bets.fifa.client.FifaHttpClient;
import net.friendly_bets.fifa.config.FifaProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Wc26FifaLiveService {

    private static final List<String> GROUP_LETTERS = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
    );
    private static final String FIFA_STANDINGS_URL =
            "https://www.fifa.com/en/tournaments/mens/worldcup/canadamexicousa2026/standings";

    private final FifaHttpClient fifaHttpClient;
    private final FifaProperties fifaProperties;

    private final Object cacheLock = new Object();
    private volatile CachedData cache;

    public Wc26FifaStandingsPageDto getStandings(String groupFilter) {
        CachedData cached = loadData();
        List<Wc26FifaGroupTableDto> groups = buildGroupTables(cached.standings());
        List<Wc26FifaBestThirdRowDto> bestThirdPlaces = buildBestThirdPlaces(groups);
        applyQualificationStatus(groups, bestThirdPlaces);
        if (groupFilter != null && !groupFilter.isBlank() && !"all".equalsIgnoreCase(groupFilter)) {
            String letter = groupFilter.trim().toUpperCase(Locale.ROOT);
            groups = groups.stream()
                    .filter(g -> letter.equals(g.getGroup()))
                    .toList();
        }
        return Wc26FifaStandingsPageDto.builder()
                .groups(groups)
                .bestThirdPlaces(bestThirdPlaces)
                .fetchedAt(cached.fetchedAt())
                .sourceUrl(FIFA_STANDINGS_URL)
                .build();
    }

    public Wc26FifaBracketPageDto getBracket(String stageFilter) {
        CachedData cached = loadData();
        List<Wc26FifaBracketMatchDto> matches = cached.matches().stream()
                .filter(m -> !FifaMatchParser.isGroupStage(m))
                .map(this::toBracketMatch)
                .filter(m -> m.getStage() != null)
                .sorted(Comparator.comparingInt(Wc26FifaBracketMatchDto::getMatchNumber))
                .toList();
        if (stageFilter != null && !stageFilter.isBlank() && !"all".equalsIgnoreCase(stageFilter)) {
            String stage = stageFilter.trim().toLowerCase(Locale.ROOT);
            matches = matches.stream()
                    .filter(m -> stage.equals(m.getStage()))
                    .toList();
        }
        return Wc26FifaBracketPageDto.builder()
                .matches(matches)
                .fetchedAt(cached.fetchedAt())
                .sourceUrl(FIFA_STANDINGS_URL.replace("/standings", "/articles/match-schedule-fixtures-results-teams-stadiums"))
                .build();
    }

    private CachedData loadData() {
        long now = System.currentTimeMillis();
        CachedData current = cache;
        if (current != null && current.expiresAtMs() > now) {
            return current;
        }
        synchronized (cacheLock) {
            current = cache;
            if (current != null && current.expiresAtMs() > System.currentTimeMillis()) {
                return current;
            }
            List<JsonNode> matches;
            JsonNode standings;
            try {
                matches = fifaHttpClient.fetchAllCalendarMatches();
                standings = fifaHttpClient.fetchGroupStageStandings();
            } catch (Exception e) {
                throw new BadRequestException("wc26FifaStandingsLoadError");
            }
            if (matches == null || matches.isEmpty() || standings == null || standings.get("Results") == null) {
                throw new BadRequestException("wc26FifaStandingsLoadError");
            }
            LocalDateTime fetchedAt = LocalDateTime.now();
            long expiresAt = System.currentTimeMillis() + fifaProperties.getCacheTtlMs();
            cache = new CachedData(matches, standings, fetchedAt, expiresAt);
            return cache;
        }
    }

    private List<Wc26FifaGroupTableDto> buildGroupTables(JsonNode standingsRoot) {
        JsonNode results = standingsRoot.get("Results");
        if (results == null || !results.isArray()) {
            throw new BadRequestException("wc26FifaStandingsLoadError");
        }

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

        List<Wc26FifaGroupTableDto> result = new ArrayList<>();
        for (String letter : GROUP_LETTERS) {
            List<JsonNode> rows = new ArrayList<>(byGroup.get(letter));
            rows.sort(Comparator.comparingInt(FifaStandingParser::position));
            List<Wc26FifaStandingRowDto> dtoRows = rows.stream()
                    .map(this::toStandingRow)
                    .collect(Collectors.toCollection(ArrayList::new));
            result.add(Wc26FifaGroupTableDto.builder()
                    .group(letter)
                    .rows(dtoRows)
                    .build());
        }
        return result;
    }

    private List<Wc26FifaBestThirdRowDto> buildBestThirdPlaces(List<Wc26FifaGroupTableDto> groups) {
        List<Wc26FifaBestThirdRowDto> thirds = new ArrayList<>();
        for (Wc26FifaGroupTableDto group : groups) {
            if (group.getRows() == null || group.getRows().size() < 3) {
                continue;
            }
            Wc26FifaStandingRowDto third = group.getRows().get(2);
            thirds.add(Wc26FifaBestThirdRowDto.builder()
                    .group(group.getGroup())
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
        thirds.sort(Wc26FifaLiveService::compareBestThirdRows);
        for (int i = 0; i < thirds.size(); i++) {
            Wc26FifaBestThirdRowDto row = thirds.get(i);
            row.setRank(i + 1);
            row.setQualifies(i < 8);
        }
        return thirds;
    }

    private static void applyQualificationStatus(
            List<Wc26FifaGroupTableDto> groups,
            List<Wc26FifaBestThirdRowDto> bestThirdPlaces
    ) {
        Map<String, Boolean> qualifyingThirds = new LinkedHashMap<>();
        for (Wc26FifaBestThirdRowDto third : bestThirdPlaces) {
            if (third.isQualifies()) {
                qualifyingThirds.put(third.getGroup() + "|" + third.getFifaCode(), true);
            }
        }
        for (Wc26FifaGroupTableDto group : groups) {
            if (group.getRows() == null) {
                continue;
            }
            for (Wc26FifaStandingRowDto row : group.getRows()) {
                if (row.getRank() <= 2) {
                    row.setQualificationStatus("direct");
                } else if (row.getRank() == 3
                        && qualifyingThirds.containsKey(group.getGroup() + "|" + row.getFifaCode())) {
                    row.setQualificationStatus("best_third");
                } else if (row.getPlayed() > 0) {
                    row.setQualificationStatus("eliminated");
                } else {
                    row.setQualificationStatus("pending");
                }
            }
        }
    }

    private Wc26FifaStandingRowDto toStandingRow(JsonNode row) {
        return Wc26FifaStandingRowDto.builder()
                .rank(FifaStandingParser.position(row))
                .fifaCode(FifaStandingParser.teamCode(row))
                .played(FifaStandingParser.played(row))
                .wins(FifaStandingParser.wins(row))
                .draws(FifaStandingParser.draws(row))
                .losses(FifaStandingParser.losses(row))
                .goalsFor(FifaStandingParser.goalsFor(row))
                .goalsAgainst(FifaStandingParser.goalsAgainst(row))
                .goalDifference(FifaStandingParser.goalDifference(row))
                .points(FifaStandingParser.points(row))
                .form(FifaStandingParser.recentForm(row))
                .qualificationStatus("pending")
                .liveNow(FifaStandingParser.liveNow(row))
                .build();
    }

    private static int compareBestThirdRows(Wc26FifaBestThirdRowDto a, Wc26FifaBestThirdRowDto b) {
        int byPoints = Integer.compare(b.getPoints(), a.getPoints());
        if (byPoints != 0) {
            return byPoints;
        }
        int byGoalDifference = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
        if (byGoalDifference != 0) {
            return byGoalDifference;
        }
        int byGoalsFor = Integer.compare(b.getGoalsFor(), a.getGoalsFor());
        if (byGoalsFor != 0) {
            return byGoalsFor;
        }
        return a.getGroup().compareTo(b.getGroup());
    }

    private Wc26FifaBracketMatchDto toBracketMatch(JsonNode match) {
        String stage = FifaMatchParser.mapKnockoutStage(FifaMatchParser.stageDescription(match));
        JsonNode home = match.get("Home");
        JsonNode away = match.get("Away");
        return Wc26FifaBracketMatchDto.builder()
                .matchNumber(FifaMatchParser.matchNumber(match))
                .stage(stage)
                .homeFifaCode(FifaMatchParser.teamCode(home))
                .awayFifaCode(FifaMatchParser.teamCode(away))
                .placeholderHome(FifaMatchParser.placeholderHome(match))
                .placeholderAway(FifaMatchParser.placeholderAway(match))
                .homeScore(FifaMatchParser.homeScore(match))
                .awayScore(FifaMatchParser.awayScore(match))
                .homePenaltyScore(intOrNull(match.get("HomeTeamPenaltyScore")))
                .awayPenaltyScore(intOrNull(match.get("AwayTeamPenaltyScore")))
                .winnerFifaCode(FifaMatchParser.winnerCode(match))
                .status(FifaMatchParser.mappedStatus(match))
                .liveMinuteLabel(FifaMatchParser.liveMinuteLabel(match))
                .utcDate(FifaMatchParser.utcDate(match))
                .build();
    }

    private static Integer intOrNull(JsonNode node) {
        return node != null && node.isNumber() ? node.asInt() : null;
    }

    private record CachedData(List<JsonNode> matches, JsonNode standings, LocalDateTime fetchedAt, long expiresAtMs) {
    }
}
