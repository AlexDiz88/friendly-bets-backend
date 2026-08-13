package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.LeagueStandingRowDto;
import net.friendly_bets.dto.LeagueStandingsPageDto;
import net.friendly_bets.dto.StandingZoneRuleDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.TeamStandingRow;
import net.friendly_bets.models.schedule.TeamStandings;
import net.friendly_bets.repositories.TeamStandingsRepository;
import net.friendly_bets.repositories.TeamsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeagueStandingsService {

    private final MatchScheduleQueryService matchScheduleQueryService;
    private final TeamStandingsRepository teamStandingsRepository;
    private final TeamsRepository teamsRepository;

    public LeagueStandingsPageDto getStandings(String leagueCodeRaw, String seasonYear, String leagueId) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        Season season = matchScheduleQueryService.resolveSeason(seasonYear);
        League league = resolveLeague(season, leagueCode, leagueId);
        return teamStandingsRepository
                .findBySeasonIdAndLeagueId(season.getId(), league.getId())
                .map(standings -> toPageDto(standings, season, league, leagueCode))
                .orElseGet(() -> emptyPageDto(season, league, leagueCode));
    }

    private LeagueStandingsPageDto toPageDto(
            TeamStandings standings,
            Season season,
            League league,
            League.LeagueCode leagueCode
    ) {
        Map<String, Team> teamsById = loadTeams(standings.getRows());
        List<LeagueStandingRowDto> rows = new ArrayList<>();
        for (TeamStandingRow row : standings.getRows()) {
            if (row == null) {
                continue;
            }
            Team team = row.getTeamId() != null ? teamsById.get(row.getTeamId()) : null;
            rows.add(LeagueStandingRowDto.from(row, team));
        }

        List<StandingZoneRuleDto> zoneRules = standings.getZoneRules() == null
                ? List.of()
                : standings.getZoneRules().stream()
                        .map(StandingZoneRuleDto::from)
                        .filter(Objects::nonNull)
                        .toList();

        return LeagueStandingsPageDto.builder()
                .seasonId(season.getId())
                .leagueId(league.getId())
                .leagueCode(leagueCode.name())
                .provider(standings.getProvider())
                .sourceUrl(standings.getSourceUrl())
                .zoneRules(zoneRules)
                .rows(rows)
                .updatedAt(standings.getUpdatedAt())
                .build();
    }

    private static LeagueStandingsPageDto emptyPageDto(
            Season season,
            League league,
            League.LeagueCode leagueCode
    ) {
        return LeagueStandingsPageDto.builder()
                .seasonId(season.getId())
                .leagueId(league.getId())
                .leagueCode(leagueCode.name())
                .rows(List.of())
                .zoneRules(List.of())
                .build();
    }

    private Map<String, Team> loadTeams(List<TeamStandingRow> rows) {
        Map<String, Team> teamsById = new HashMap<>();
        if (rows == null) {
            return teamsById;
        }
        for (TeamStandingRow row : rows) {
            if (row == null || row.getTeamId() == null || teamsById.containsKey(row.getTeamId())) {
                continue;
            }
            teamsRepository.findById(row.getTeamId()).ifPresent(team -> teamsById.put(team.getId(), team));
        }
        return teamsById;
    }

    private static League resolveLeague(Season season, League.LeagueCode leagueCode, String leagueId) {
        if (season.getLeagues() == null || season.getLeagues().isEmpty()) {
            throw new BadRequestException("leagueNotFoundInSeason");
        }
        if (leagueId != null && !leagueId.isBlank()) {
            return season.getLeagues().stream()
                    .filter(Objects::nonNull)
                    .filter(l -> leagueId.equals(l.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        }
        return season.getLeagues().stream()
                .filter(Objects::nonNull)
                .filter(l -> leagueCode.equals(l.getLeagueCode()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
    }

    private static League.LeagueCode parseLeagueCode(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }
}
