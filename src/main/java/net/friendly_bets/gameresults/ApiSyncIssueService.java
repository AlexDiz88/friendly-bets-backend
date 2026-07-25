package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.schedule.MatchSchedule;
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
            MatchSchedule match,
            String leagueCode,
            String season,
            int matchday,
            String message
    ) {
        if (match == null || match.getId() == null) {
            return;
        }
        if (apiSyncIssueRepository.existsByProviderAndIssueTypeAndMatchScheduleId(
                "marathonbet",
                ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name(),
                match.getId())) {
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider("marathonbet")
                .issueType(ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .matchScheduleId(match.getId())
                .homeTeamName(resolveTeamName(match, true))
                .awayTeamName(resolveTeamName(match, false))
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
        if ("soccer365.ru".equals(provider)) {
            return teamAliasResolver.resolveSoccer365ByName(externalName).isPresent();
        }
        return false;
    }

    public void recordTeamMappingMissing(
            String provider,
            String leagueCode,
            String season,
            Integer matchday,
            String homeTeamName,
            String awayTeamName,
            String message
    ) {
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(provider)
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .homeTeamName(homeTeamName)
                .awayTeamName(awayTeamName)
                .message(message)
                .build());
    }

    private static boolean namesMatch(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private String resolveTeamName(MatchSchedule match, boolean home) {
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        if (teamId != null && !teamId.isBlank()) {
            return teamsRepository.findById(teamId).map(Team::getTitle).orElse(null);
        }
        return null;
    }
}
