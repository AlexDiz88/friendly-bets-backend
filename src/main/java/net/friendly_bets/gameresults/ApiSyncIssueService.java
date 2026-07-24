package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records mapping/fetch issues for Marathonbet (and team-alias purge). Match-result provider sync removed.
 */
@Service
@RequiredArgsConstructor
public class ApiSyncIssueService {

    private final ApiSyncIssueRepository apiSyncIssueRepository;
    private final TeamsRepository teamsRepository;
    private final TeamAliasResolver teamAliasResolver;

    public void recordMarathonbetEventMappingMissing(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            String message
    ) {
        if (match == null || match.getId() == null) {
            return;
        }
        if (apiSyncIssueRepository.existsByProviderAndIssueTypeAndGameResultId(
                "marathonbet",
                ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name(),
                match.getId())) {
            return;
        }
        GameResultSourceSnapshot source = match.primaryExternalSource();
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider("marathonbet")
                .issueType(ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .gameResultId(match.getId())
                .externalMatchId(source != null ? source.getExternalMatchId() : null)
                .homeTeamName(resolveTeamName(match, source, true))
                .awayTeamName(resolveTeamName(match, source, false))
                .message(message)
                .build());
    }

    public void recordMarathonbetFetchFailed(String leagueCode, String season, String message) {
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider("marathonbet")
                .issueType(ApiSyncIssue.IssueType.MARATHONBET_FETCH_FAILED.name())
                .leagueCode(leagueCode)
                .season(season)
                .message(message)
                .build());
    }

    public void recordMarathonbetPrimaryUnavailable(String leagueCode, String season, String message) {
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider("marathonbet")
                .issueType(ApiSyncIssue.IssueType.PRIMARY_PROVIDER_UNAVAILABLE.name())
                .leagueCode(leagueCode)
                .season(season)
                .message(message)
                .build());
    }

    public int purgeTeamMappingIssuesForExternalTeam(String provider, String externalName, Integer externalId) {
        if (!isExternalTeamMapped(provider, externalId, externalName)) {
            return 0;
        }
        List<String> toDelete = new ArrayList<>();
        for (ApiSyncIssue issue : apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (provider != null && !provider.equals(issue.getProvider())) {
                continue;
            }
            if (namesMatch(externalName, issue.getHomeTeamName()) || namesMatch(externalName, issue.getAwayTeamName())) {
                toDelete.add(issue.getId());
            }
        }
        if (!toDelete.isEmpty()) {
            apiSyncIssueRepository.deleteAllById(toDelete);
        }
        return toDelete.size();
    }

    private boolean isExternalTeamMapped(String provider, Integer externalId, String externalName) {
        if ("marathonbet".equals(provider)) {
            return teamAliasResolver.resolveMarathonbetByName(externalName).isPresent();
        }
        if ("4score.ru".equals(provider)) {
            return teamAliasResolver.resolveFourScoreByName(externalName).isPresent();
        }
        if ("24score.pro".equals(provider)) {
            return teamAliasResolver.resolveTwentyFourScoreByName(externalName).isPresent();
        }
        if ("odds-api.io".equals(provider)) {
            return teamAliasResolver.oddsApiAliasesMapped(externalId, externalName);
        }
        return false;
    }

    private static boolean namesMatch(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private String resolveTeamName(GameResultRecord match, GameResultSourceSnapshot source, boolean home) {
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        if (teamId != null && !teamId.isBlank()) {
            return teamsRepository.findById(teamId).map(Team::getTitle).orElse(null);
        }
        GameResultSideSnapshot side = source == null ? null : (home ? source.getHome() : source.getAway());
        return side != null ? side.getExternalName() : null;
    }
}
