package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.models.Team;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FourScoreTeamResolver {

    private final TeamAliasResolver teamAliasResolver;
    private final ApiSyncIssueService apiSyncIssueService;

    public Optional<Team> resolve(String fourScoreName) {
        if (fourScoreName == null || fourScoreName.isBlank()) {
            return Optional.empty();
        }
        return teamAliasResolver.resolveFourScoreByName(fourScoreName.trim());
    }

    public Optional<Team> resolveOrRecordMissing(
            String fourScoreName,
            String leagueCode,
            String season,
            int matchday,
            Long externalEventId,
            boolean home
    ) {
        if (FourScorePlayoffPlaceholderNames.isPlaceholder(fourScoreName)) {
            return Optional.empty();
        }
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
}
