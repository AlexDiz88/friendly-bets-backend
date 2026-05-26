package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.external.ExternalSyncIssue;
import net.friendly_bets.repositories.ExternalSyncIssueRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalSyncIssueService {

    private final ExternalSyncIssueRepository externalSyncIssueRepository;
    private final TeamAliasResolver teamAliasResolver;

    public void recordMissingTeamMapping(
            String competitionCode,
            String season,
            int matchday,
            FootballDataMatchDto matchDto
    ) {
        String homeName = matchDto != null && matchDto.getHomeTeam() != null ? matchDto.getHomeTeam().getName() : null;
        String awayName = matchDto != null && matchDto.getAwayTeam() != null ? matchDto.getAwayTeam().getName() : null;
        Integer homeId = matchDto != null && matchDto.getHomeTeam() != null ? matchDto.getHomeTeam().getId() : null;
        Integer awayId = matchDto != null && matchDto.getAwayTeam() != null ? matchDto.getAwayTeam().getId() : null;

        externalSyncIssueRepository.save(ExternalSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(ExternalSyncIssue.Provider.FOOTBALL_DATA.name())
                .issueType(ExternalSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .competitionCode(competitionCode)
                .season(season)
                .matchday(matchday)
                .externalMatchId(matchDto != null ? matchDto.getId() : null)
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .homeTeamExternalId(homeId)
                .awayTeamExternalId(awayId)
                .message("Missing team mapping for external match (configure Team external aliases)")
                .build());
    }

    public List<ExternalSyncIssue> getLatest() {
        return externalSyncIssueRepository.findTop200ByOrderByCreatedAtDesc();
    }

    public void clearAll() {
        externalSyncIssueRepository.deleteAll();
    }

    /**
     * Unique API team names from recent sync issues that are still not mapped to a {@link net.friendly_bets.models.Team}.
     */
    public List<UnmappedExternalTeamNameDto> getUnmappedTeamNameHints() {
        Map<String, UnmappedExternalTeamNameDto> byName = new LinkedHashMap<>();
        for (ExternalSyncIssue issue : getLatest()) {
            if (!ExternalSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            addUnmappedHint(byName, issue.getHomeTeamName(), issue.getHomeTeamExternalId());
            addUnmappedHint(byName, issue.getAwayTeamName(), issue.getAwayTeamExternalId());
        }
        return new ArrayList<>(byName.values());
    }

    private void addUnmappedHint(Map<String, UnmappedExternalTeamNameDto> byName, String name, Integer externalId) {
        if (name == null || name.isBlank()) {
            return;
        }
        int fdId = externalId != null ? externalId : 0;
        if (teamAliasResolver.resolveFootballData(fdId, name).isPresent()) {
            return;
        }
        byName.merge(
                name,
                UnmappedExternalTeamNameDto.builder()
                        .externalName(name)
                        .externalId(externalId)
                        .build(),
                (existing, incoming) -> existing.getExternalId() == null && incoming.getExternalId() != null
                        ? incoming
                        : existing
        );
    }
}

