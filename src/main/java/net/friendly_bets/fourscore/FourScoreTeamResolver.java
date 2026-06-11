package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.oddsapi.TeamNameNormalizer;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FourScoreTeamResolver {

    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;
    private final ApiSyncIssueService apiSyncIssueService;

    public Optional<Team> resolve(String fourScoreName) {
        if (fourScoreName == null || fourScoreName.isBlank()) {
            return Optional.empty();
        }
        String name = fourScoreName.trim();
        Optional<Team> byAlias = teamAliasResolver.resolveFourScoreByName(name);
        if (byAlias.isPresent()) {
            return byAlias;
        }
        Optional<String> fifaCode = FourScoreTeamCatalog.fifaCodeForFourScoreName(name);
        if (fifaCode.isPresent()) {
            Optional<Team> byCode = teamAliasResolver.resolveWc26Code(fifaCode.get());
            if (byCode.isPresent()) {
                return byCode;
            }
        }
        return fuzzyMatch(name);
    }

    public Optional<Team> resolveOrRecordMissing(
            String fourScoreName,
            String leagueCode,
            String season,
            int matchday,
            Long externalEventId,
            boolean home
    ) {
        Optional<Team> team = resolve(fourScoreName);
        if (team.isEmpty()) {
            apiSyncIssueService.recordMissingFourScoreTeamMapping(
                    leagueCode,
                    season,
                    matchday,
                    externalEventId,
                    home,
                    fourScoreName
            );
        }
        return team;
    }

    private Optional<Team> fuzzyMatch(String fourScoreName) {
        String normalized = TeamNameNormalizer.normalize(fourScoreName);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        List<Team> teams = teamsRepository.findAll();
        for (Team team : teams) {
            if (matchesTeam(team, normalized)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    private static boolean matchesTeam(Team team, String normalizedQuery) {
        if (team.getTitle() != null) {
            String titleNorm = TeamNameNormalizer.normalize(team.getTitle());
            if (!titleNorm.isBlank()
                    && (titleNorm.equals(normalizedQuery)
                    || titleNorm.contains(normalizedQuery)
                    || normalizedQuery.contains(titleNorm))) {
                return true;
            }
        }
        TeamDisplayNames names = team.getDisplayNames();
        if (names == null) {
            return false;
        }
        for (String candidate : List.of(names.getEn(), names.getRu(), names.getDe())) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String norm = TeamNameNormalizer.normalize(candidate);
            if (norm.equals(normalizedQuery) || norm.contains(normalizedQuery) || normalizedQuery.contains(norm)) {
                return true;
            }
        }
        return false;
    }
}
