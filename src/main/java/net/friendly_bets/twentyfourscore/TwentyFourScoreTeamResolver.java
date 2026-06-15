package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.models.Team;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreTeamResolver {

    private final TeamAliasResolver teamAliasResolver;
    private final ApiSyncIssueService apiSyncIssueService;

    public Optional<Team> resolve(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return Optional.empty();
        }
        return teamAliasResolver.resolveTwentyFourScoreByName(teamName.trim());
    }

    public Optional<Team> resolveOrRecordMissing(
            String teamName,
            String leagueCode,
            String season,
            int matchday,
            Long externalMatchId,
            boolean home
    ) {
        Optional<Team> team = resolve(teamName);
        if (team.isEmpty()) {
            apiSyncIssueService.recordMissingTwentyFourScoreTeamMapping(
                    leagueCode,
                    season,
                    matchday,
                    externalMatchId,
                    home,
                    teamName
            );
        }
        return team;
    }
}
