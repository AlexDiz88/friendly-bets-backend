package net.friendly_bets.tournamentarchive;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26FifaBestThirdRowDto;
import net.friendly_bets.dto.Wc26FifaBracketMatchDto;
import net.friendly_bets.dto.Wc26FifaBracketPageDto;
import net.friendly_bets.dto.Wc26FifaGroupTableDto;
import net.friendly_bets.dto.Wc26FifaStandingRowDto;
import net.friendly_bets.dto.Wc26FifaStandingsPageDto;
import net.friendly_bets.dto.Wc26ScheduleMatchDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveBestThirdRow;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveBracketPair;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveMatch;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveStandingRow;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Проекции архива в существующие DTO страницы ЧМ (schedule / standings / bracket).
 */
@Service
@RequiredArgsConstructor
public class TournamentArchiveViewService {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_LOCAL = DateTimeFormatter.ofPattern("HH:mm");

    private final TournamentArchiveService tournamentArchiveService;
    private final TeamsRepository teamsRepository;

    public Wc26SchedulePageDto schedulePage(String editionCode) {
        TournamentArchive archive = tournamentArchiveService.getByEditionCode(editionCode);
        Map<String, Team> teams = loadTeams(archive);
        List<Wc26ScheduleMatchDto> matches = archive.getMatches().stream()
                .sorted(Comparator.comparingInt(TournamentArchiveMatch::getMatchNumber))
                .map(m -> toScheduleMatch(m, teams))
                .toList();
        return Wc26SchedulePageDto.builder().matches(matches).build();
    }

    public Wc26FifaStandingsPageDto standingsPage(String editionCode, String groupFilter) {
        TournamentArchive archive = tournamentArchiveService.getByEditionCode(editionCode);
        Map<String, Team> teams = loadTeams(archive);
        Map<String, List<TournamentArchiveStandingRow>> byGroup = new LinkedHashMap<>();
        for (TournamentArchiveStandingRow row : archive.getGroupStandings()) {
            byGroup.computeIfAbsent(row.getGroup(), k -> new ArrayList<>()).add(row);
        }
        List<Wc26FifaGroupTableDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<TournamentArchiveStandingRow>> entry : byGroup.entrySet()) {
            List<Wc26FifaStandingRowDto> rows = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(TournamentArchiveStandingRow::getRank))
                    .map(r -> toStandingRow(r, teams))
                    .collect(Collectors.toCollection(ArrayList::new));
            groups.add(Wc26FifaGroupTableDto.builder().group(entry.getKey()).rows(rows).build());
        }
        if (groupFilter != null && !groupFilter.isBlank() && !"all".equalsIgnoreCase(groupFilter)) {
            String letter = groupFilter.trim().toUpperCase(Locale.ROOT);
            groups = groups.stream().filter(g -> letter.equals(g.getGroup())).toList();
        }
        List<Wc26FifaBestThirdRowDto> bestThirds = archive.getBestThirdPlaces().stream()
                .sorted(Comparator.comparingInt(TournamentArchiveBestThirdRow::getRank))
                .map(r -> toBestThird(r, teams))
                .toList();
        return Wc26FifaStandingsPageDto.builder()
                .groups(groups)
                .bestThirdPlaces(bestThirds)
                .fetchedAt(archive.getImportedAt() != null ? archive.getImportedAt() : archive.getExportedAt())
                .sourceUrl("tournament_archives:" + archive.getEditionCode())
                .build();
    }

    public Wc26FifaBracketPageDto bracketPage(String editionCode, String stageFilter) {
        TournamentArchive archive = tournamentArchiveService.getByEditionCode(editionCode);
        Map<String, Team> teams = loadTeams(archive);
        Map<Integer, TournamentArchiveBracketPair> bracketByMatch = new HashMap<>();
        if (archive.getBracket() != null) {
            for (TournamentArchiveBracketPair pair : archive.getBracket()) {
                bracketByMatch.put(pair.getMatchNumber(), pair);
            }
        }
        List<TournamentArchiveMatch> matches = tournamentArchiveService.knockoutMatches(editionCode, stageFilter);
        List<Wc26FifaBracketMatchDto> dtos = matches.stream()
                .map(m -> toBracketMatch(m, teams, bracketByMatch.get(m.getMatchNumber())))
                .toList();
        return Wc26FifaBracketPageDto.builder()
                .matches(dtos)
                .fetchedAt(archive.getImportedAt() != null ? archive.getImportedAt() : archive.getExportedAt())
                .sourceUrl("tournament_archives:" + archive.getEditionCode())
                .build();
    }

    private Map<String, Team> loadTeams(TournamentArchive archive) {
        Map<String, Team> map = new HashMap<>();
        List<String> ids = new ArrayList<>();
        if (archive.getMatches() != null) {
            for (TournamentArchiveMatch m : archive.getMatches()) {
                if (m.getHomeTeamId() != null) {
                    ids.add(m.getHomeTeamId());
                }
                if (m.getAwayTeamId() != null) {
                    ids.add(m.getAwayTeamId());
                }
                if (m.getWinnerTeamId() != null) {
                    ids.add(m.getWinnerTeamId());
                }
            }
        }
        if (archive.getGroupStandings() != null) {
            for (TournamentArchiveStandingRow row : archive.getGroupStandings()) {
                if (row.getTeamId() != null) {
                    ids.add(row.getTeamId());
                }
            }
        }
        for (Team team : teamsRepository.findAllById(ids)) {
            map.put(team.getId(), team);
        }
        return map;
    }

    private Wc26ScheduleMatchDto toScheduleMatch(TournamentArchiveMatch m, Map<String, Team> teams) {
        boolean hasScore = hasAnyScore(m.getGameScore());
        String dateLocal = null;
        String timeLocal = null;
        if (m.getKickoffUtc() != null) {
            ZonedDateTime berlin = m.getKickoffUtc().atZone(UTC).withZoneSameInstant(BERLIN);
            dateLocal = berlin.toLocalDate().toString();
            timeLocal = berlin.toLocalTime().format(TIME_LOCAL);
        }
        return Wc26ScheduleMatchDto.builder()
                .id(m.getMatchNumber())
                .date(dateLocal)
                .timeLocal(timeLocal)
                .stage(TournamentArchiveStages.toUiStage(m.getStage()))
                .group(m.getGroup())
                .home(fifaOf(m.getHomeTeamId(), teams))
                .away(fifaOf(m.getAwayTeamId(), teams))
                .kickoffUtc(m.getKickoffUtc())
                .scoreView(TournamentArchiveService.formatScoreView(m.getGameScore()))
                .status(hasScore ? "FINISHED" : "SCHEDULED")
                .finalized(hasScore)
                .utcDate(m.getKickoffUtc())
                .build();
    }

    private Wc26FifaStandingRowDto toStandingRow(TournamentArchiveStandingRow row, Map<String, Team> teams) {
        String fifa = row.getFifaCode();
        if (fifa == null || fifa.isBlank()) {
            fifa = fifaOf(row.getTeamId(), teams);
        }
        return Wc26FifaStandingRowDto.builder()
                .rank(row.getRank())
                .fifaCode(fifa)
                .played(row.getPlayed())
                .wins(row.getWins())
                .draws(row.getDraws())
                .losses(row.getLosses())
                .goalsFor(row.getGoalsFor())
                .goalsAgainst(row.getGoalsAgainst())
                .goalDifference(row.getGoalDifference())
                .points(row.getPoints())
                .form(List.of())
                .qualificationStatus(row.getQualificationStatus())
                .liveNow(false)
                .build();
    }

    private Wc26FifaBestThirdRowDto toBestThird(TournamentArchiveBestThirdRow row, Map<String, Team> teams) {
        String fifa = row.getFifaCode();
        if (fifa == null || fifa.isBlank()) {
            fifa = fifaOf(row.getTeamId(), teams);
        }
        return Wc26FifaBestThirdRowDto.builder()
                .rank(row.getRank())
                .group(row.getGroup())
                .fifaCode(fifa)
                .played(row.getPlayed())
                .wins(row.getWins())
                .draws(row.getDraws())
                .losses(row.getLosses())
                .points(row.getPoints())
                .goalDifference(row.getGoalDifference())
                .goalsFor(row.getGoalsFor())
                .goalsAgainst(row.getGoalsAgainst())
                .qualifies(row.isQualifies())
                .build();
    }

    private Wc26FifaBracketMatchDto toBracketMatch(
            TournamentArchiveMatch m,
            Map<String, Team> teams,
            TournamentArchiveBracketPair pair
    ) {
        GameScore score = m.getGameScore();
        Integer homeScore = null;
        Integer awayScore = null;
        Integer homePen = null;
        Integer awayPen = null;
        if (score != null) {
            int[] openPlay = preferOpenPlay(score);
            if (openPlay != null) {
                homeScore = openPlay[0];
                awayScore = openPlay[1];
            }
            int[] pen = parsePart(score.getPenalty());
            if (pen != null) {
                homePen = pen[0];
                awayPen = pen[1];
            }
        }
        String placeholderHome = null;
        String placeholderAway = null;
        if (pair != null) {
            boolean runnerUp = "runner_up".equalsIgnoreCase(pair.getFrom());
            placeholderHome = pair.getHome() != null
                    ? (runnerUp ? "RU" + pair.getHome() : "W" + pair.getHome())
                    : null;
            placeholderAway = pair.getAway() != null
                    ? (runnerUp ? "RU" + pair.getAway() : "W" + pair.getAway())
                    : null;
        }
        return Wc26FifaBracketMatchDto.builder()
                .matchNumber(m.getMatchNumber())
                .stage(TournamentArchiveStages.toUiStage(m.getStage()))
                .homeFifaCode(fifaOf(m.getHomeTeamId(), teams))
                .awayFifaCode(fifaOf(m.getAwayTeamId(), teams))
                .placeholderHome(placeholderHome)
                .placeholderAway(placeholderAway)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .homePenaltyScore(homePen)
                .awayPenaltyScore(awayPen)
                .winnerFifaCode(fifaOf(m.getWinnerTeamId(), teams))
                .status(homeScore != null || homePen != null ? "FINISHED" : "SCHEDULED")
                .utcDate(m.getKickoffUtc())
                .build();
    }

    /** Team.country в БД → FIFA TLA для WC26 UI. */
    private static final Map<String, String> COUNTRY_TO_FIFA = Map.of(
            "SPA", "ESP",
            "MOR", "MAR",
            "SWI", "SUI",
            "CUR", "CUW"
    );

    private static String fifaOf(String teamId, Map<String, Team> teams) {
        if (teamId == null) {
            return null;
        }
        Team team = teams.get(teamId);
        if (team == null) {
            return null;
        }
        if (team.getCountry() != null && !team.getCountry().isBlank()) {
            String country = team.getCountry().trim().toUpperCase(Locale.ROOT);
            return COUNTRY_TO_FIFA.getOrDefault(country, country);
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle()).orElse(null);
    }

    private static boolean hasAnyScore(GameScore score) {
        if (score == null) {
            return false;
        }
        return notBlank(score.getFullTime())
                || notBlank(score.getOverTime())
                || notBlank(score.getPenalty());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static int[] preferOpenPlay(GameScore score) {
        int[] ot = parsePart(score.getOverTime());
        if (ot != null) {
            return ot;
        }
        return parsePart(score.getFullTime());
    }

    private static int[] parsePart(String part) {
        if (part == null || part.isBlank() || !part.contains(":")) {
            return null;
        }
        String[] bits = part.trim().split(":");
        if (bits.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(bits[0]), Integer.parseInt(bits[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
