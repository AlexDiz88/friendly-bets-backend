package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
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

    public void recordOddsEventMappingMissing(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday
    ) {
        recordOddsEventMappingMissing(match, leagueCode, season, matchday, null);
    }

    public void recordOddsEventMappingMissing(
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
                MatchDataProviders.ODDS_API,
                ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name(),
                match.getId())) {
            return;
        }
        GameResultSourceSnapshot source = match.footballDataSource();
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.ODDS_API)
                .issueType(ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .gameResultId(match.getId())
                .externalMatchId(source != null ? source.getExternalMatchId() : null)
                .homeTeamName(sideName(source, true))
                .awayTeamName(sideName(source, false))
                .message(message)
                .build());
    }

    public void recordOddsTeamMappingMissing(
            GameResultRecord match,
            boolean home,
            String oddsApiTeamName,
            Integer oddsApiTeamId
    ) {
        recordUnmappedOddsApiTeamNameHint(oddsApiTeamName, oddsApiTeamId, home, match);
    }

    /**
     * Hint for admin team form chips ({@code UnmappedTeamNameHints}). Safe to call from odds demo refresh.
     */
    public void recordUnmappedOddsApiTeamNameHint(
            String oddsApiTeamName,
            Integer oddsApiTeamId,
            boolean home,
            GameResultRecord match
    ) {
        if (oddsApiTeamName == null || oddsApiTeamName.isBlank()) {
            return;
        }
        String externalId = oddsApiTeamId != null ? String.valueOf(oddsApiTeamId) : null;
        if (teamAliasResolver.resolveOddsApi(oddsApiTeamId, oddsApiTeamName).isPresent()) {
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.ODDS_API)
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .leagueCode(match != null ? match.getLeagueCode() : null)
                .season(match != null ? match.getSeason() : null)
                .matchday(match != null ? match.getMatchday() : null)
                .gameResultId(match != null ? match.getId() : null)
                .homeTeamName(home ? oddsApiTeamName : null)
                .awayTeamName(home ? null : oddsApiTeamName)
                .homeTeamExternalId(home ? externalId : null)
                .awayTeamExternalId(home ? null : externalId)
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
        Map<String, UnmappedExternalTeamNameDto> byKey = new LinkedHashMap<>();
        for (ApiSyncIssue issue : getLatest()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            String provider = issue.getProvider() != null
                    ? issue.getProvider()
                    : MatchDataProviders.FOOTBALL_DATA;
            addUnmappedHint(byKey, provider, issue.getHomeTeamName(), issue.getHomeTeamExternalId());
            addUnmappedHint(byKey, provider, issue.getAwayTeamName(), issue.getAwayTeamExternalId());
        }
        return new ArrayList<>(byKey.values());
    }

    private void addUnmappedHint(
            Map<String, UnmappedExternalTeamNameDto> byKey,
            String provider,
            String name,
            String externalId
    ) {
        if (name == null || name.isBlank()) {
            return;
        }
        int parsedId = parseExternalId(externalId);
        if (MatchDataProviders.ODDS_API.equals(provider)) {
            if (teamAliasResolver.resolveOddsApi(parsedId > 0 ? parsedId : null, name).isPresent()) {
                return;
            }
        } else if (teamAliasResolver.resolveFootballData(parsedId, name).isPresent()) {
            return;
        }
        byKey.merge(
                hintKey(provider, name),
                UnmappedExternalTeamNameDto.builder()
                        .externalName(name)
                        .externalId(parsedId > 0 ? parsedId : null)
                        .provider(provider)
                        .build(),
                (existing, incoming) -> existing.getExternalId() == null && incoming.getExternalId() != null
                        ? incoming
                        : existing
        );
    }

    private static String hintKey(String provider, String name) {
        return provider + "\0" + name;
    }

    private static String sideName(GameResultSourceSnapshot source, boolean home) {
        if (source == null) {
            return null;
        }
        GameResultSideSnapshot side = home ? source.getHome() : source.getAway();
        return side != null ? side.getExternalName() : null;
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
