package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiSyncIssueService {

    private final ApiSyncIssueRepository apiSyncIssueRepository;
    private final TeamAliasResolver teamAliasResolver;

    public void recordMissingTeamMapping(
            String leagueCode,
            String season,
            int matchday,
            FootballDataMatchDto matchDto
    ) {
        String homeName = matchDto != null && matchDto.getHomeTeam() != null ? matchDto.getHomeTeam().getName() : null;
        String awayName = matchDto != null && matchDto.getAwayTeam() != null ? matchDto.getAwayTeam().getName() : null;
        String homeId = matchDto != null && matchDto.getHomeTeam() != null
                ? String.valueOf(matchDto.getHomeTeam().getId()) : null;
        String awayId = matchDto != null && matchDto.getAwayTeam() != null
                ? String.valueOf(matchDto.getAwayTeam().getId()) : null;
        Long externalMatchId = matchDto != null ? matchDto.getId() : null;

        if (externalMatchId != null
                && apiSyncIssueRepository.existsByProviderAndIssueTypeAndExternalMatchId(
                MatchDataProviders.FOOTBALL_DATA,
                ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name(),
                externalMatchId)) {
            return;
        }

        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.FOOTBALL_DATA)
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .externalMatchId(externalMatchId)
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .homeTeamExternalId(homeId)
                .awayTeamExternalId(awayId)
                .build());
    }

    public List<ApiSyncIssue> getLatest() {
        return apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc();
    }

    public void clearAll() {
        apiSyncIssueRepository.deleteAll();
    }

    public boolean hasIssues() {
        return apiSyncIssueRepository.count() > 0;
    }

    public List<UnmappedExternalTeamNameDto> getUnmappedTeamNameHints() {
        Map<String, UnmappedExternalTeamNameDto> byName = new LinkedHashMap<>();
        for (ApiSyncIssue issue : getLatest()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            addUnmappedHint(byName, issue.getHomeTeamName(), issue.getHomeTeamExternalId());
            addUnmappedHint(byName, issue.getAwayTeamName(), issue.getAwayTeamExternalId());
        }
        return new ArrayList<>(byName.values());
    }

    private void addUnmappedHint(Map<String, UnmappedExternalTeamNameDto> byName, String name, String externalId) {
        if (name == null || name.isBlank()) {
            return;
        }
        int fdId = parseExternalId(externalId);
        if (teamAliasResolver.resolveFootballData(fdId, name).isPresent()) {
            return;
        }
        byName.merge(
                name,
                UnmappedExternalTeamNameDto.builder()
                        .externalName(name)
                        .externalId(fdId > 0 ? fdId : null)
                        .build(),
                (existing, incoming) -> existing.getExternalId() == null && incoming.getExternalId() != null
                        ? incoming
                        : existing
        );
    }

    private static int parseExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(externalId.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
