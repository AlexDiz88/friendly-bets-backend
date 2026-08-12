package net.friendly_bets.providers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.StandingZoneRule;
import net.friendly_bets.models.schedule.TeamStandingRow;
import net.friendly_bets.models.schedule.TeamStandings;
import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingZoneRuleSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import net.friendly_bets.repositories.TeamStandingsRepository;
import net.friendly_bets.services.ExternalTeamAliasAutoBindService;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provider-agnostic persist of {@link StandingsTableSnapshot} into {@code team_standings}.
 */
@Component
@RequiredArgsConstructor
public class StandingsPersistSupport {

    private final TeamAliasResolver teamAliasResolver;
    private final ExternalTeamAliasAutoBindService autoBindService;
    private final TeamStandingsRepository teamStandingsRepository;

    public StandingsSyncResultDto persist(
            StandingsTableSnapshot snapshot,
            String providerId,
            Season season,
            League league
    ) {
        if (snapshot == null || season == null || league == null || league.getLeagueCode() == null) {
            return StandingsSyncResultDto.builder().build();
        }
        List<String> externalNames = snapshot.getRows().stream()
                .map(StandingRowSnapshot::getExternalTeamName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        autoBindService.bindAndCollectUnmapped(
                providerId,
                league.getLeagueCode().name(),
                externalNames
        );

        List<TeamStandingRow> rows = new ArrayList<>();
        Set<String> unmapped = new LinkedHashSet<>();
        for (StandingRowSnapshot row : snapshot.getRows()) {
            if (row == null || row.getExternalTeamName() == null || row.getExternalTeamName().isBlank()) {
                continue;
            }
            Optional<Team> team = teamAliasResolver.resolveByProviderName(providerId, row.getExternalTeamName());
            if (team.isEmpty()) {
                unmapped.add(row.getExternalTeamName().trim());
                continue;
            }
            Team resolved = team.get();
            rows.add(TeamStandingRow.builder()
                    .rank(row.getRank())
                    .teamId(resolved.getId())
                    .played(row.getPlayed())
                    .wins(row.getWins())
                    .draws(row.getDraws())
                    .losses(row.getLosses())
                    .goalsFor(row.getGoalsFor())
                    .goalsAgainst(row.getGoalsAgainst())
                    .goalDifference(row.getGoalDifference())
                    .points(row.getPoints())
                    .zoneCode(row.getZoneCode())
                    .build());
        }

        List<StandingZoneRule> zoneRules = snapshot.getZoneRules().stream()
                .map(StandingsPersistSupport::toEntityZoneRule)
                .toList();

        Instant now = Instant.now();
        TeamStandings document = teamStandingsRepository
                .findBySeasonIdAndLeagueId(season.getId(), league.getId())
                .orElseGet(() -> TeamStandings.builder()
                        .seasonId(season.getId())
                        .leagueId(league.getId())
                        .build());
        document.setGroup(snapshot.getGroup());
        document.setRows(rows);
        document.setZoneRules(zoneRules);
        document.setProvider(providerId);
        document.setSourceUrl(snapshot.getSourceUrl());
        document.setUpdatedAt(now);
        teamStandingsRepository.save(document);

        return StandingsSyncResultDto.builder()
                .leagueCode(league.getLeagueCode().name())
                .seasonId(season.getId())
                .leagueId(league.getId())
                .provider(providerId)
                .rowsSaved(rows.size())
                .skippedUnmapped(unmapped.size())
                .unmappedNames(new ArrayList<>(unmapped))
                .build();
    }

    private static StandingZoneRule toEntityZoneRule(StandingZoneRuleSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return StandingZoneRule.builder()
                .code(snapshot.getCode())
                .label(snapshot.getLabel())
                .cssClass(snapshot.getCssClass())
                .build();
    }
}
