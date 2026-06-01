package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.gameresults.GameScoreValidator;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.GameScore;
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
import java.util.Optional;

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

    public void recordOddsMarketUnmapped(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            String bookmaker,
            String marketName,
            String message
    ) {
        recordOddsMappingIssue(
                match,
                leagueCode,
                season,
                matchday,
                ApiSyncIssue.IssueType.ODDS_MARKET_UNMAPPED,
                bookmaker + " · " + marketName + (message != null ? " · " + message : ""));
    }

    public void recordOddsSelectionUnmapped(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            String bookmaker,
            String marketName,
            String message
    ) {
        recordOddsMappingIssue(
                match,
                leagueCode,
                season,
                matchday,
                ApiSyncIssue.IssueType.ODDS_SELECTION_UNMAPPED,
                bookmaker + " · " + marketName + (message != null ? " · " + message : ""));
    }

    public void recordOddsQuoteMismatch(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            String message
    ) {
        recordOddsMappingIssue(
                match,
                leagueCode,
                season,
                matchday,
                ApiSyncIssue.IssueType.ODDS_QUOTE_MISMATCH,
                message);
    }

    public void recordOddsQuoteRejected(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            String bookmaker,
            String marketName,
            String message
    ) {
        recordOddsMappingIssue(
                match,
                leagueCode,
                season,
                matchday,
                ApiSyncIssue.IssueType.ODDS_QUOTE_REJECTED,
                bookmaker + " · " + marketName + (message != null ? " · " + message : ""));
    }

    private void recordOddsMappingIssue(
            GameResultRecord match,
            String leagueCode,
            String season,
            int matchday,
            ApiSyncIssue.IssueType issueType,
            String message
    ) {
        GameResultSourceSnapshot source = match != null ? match.footballDataSource() : null;
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.ODDS_API)
                .issueType(issueType.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .gameResultId(match != null ? match.getId() : null)
                .externalMatchId(source != null ? source.getExternalMatchId() : null)
                .homeTeamName(sideName(source, true))
                .awayTeamName(sideName(source, false))
                .message(message)
                .build());
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
        if (teamAliasResolver.oddsApiAliasesMapped(oddsApiTeamId, oddsApiTeamName)) {
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

    /**
     * football-data обновил матч ({@code lastUpdated} новее сохранённого), а счёт отличается от канона.
     */
    public void recordApiScoreChangedIfNeeded(GameResultRecord existing, GameResultRecord incoming) {
        if (existing == null || incoming == null || existing.getId() == null || existing.isAdminCorrected()) {
            return;
        }
        GameScore storedScore = existing.getGameScore();
        GameScore incomingScore = incoming.getGameScore();
        if (!GameScoreValidator.hasValidFullTime(storedScore) || !GameScoreValidator.hasValidFullTime(incomingScore)) {
            return;
        }
        if (GameScoreValidator.sameCanonicalScore(storedScore, incomingScore)) {
            return;
        }

        GameResultSourceSnapshot storedSource = existing.footballDataSource();
        GameResultSourceSnapshot incomingSource = incoming.footballDataSource();
        LocalDateTime storedApiLastUpdated = storedSource != null ? storedSource.getApiLastUpdated() : null;
        LocalDateTime incomingApiLastUpdated = incomingSource != null ? incomingSource.getApiLastUpdated() : null;
        if (incomingApiLastUpdated == null) {
            return;
        }
        if (storedApiLastUpdated != null && !incomingApiLastUpdated.isAfter(storedApiLastUpdated)) {
            return;
        }

        String message = buildApiScoreChangedMessage(storedScore, incomingScore, existing.isFinalized(), incomingApiLastUpdated);
        String provider = MatchDataProviders.FOOTBALL_DATA;
        String issueType = ApiSyncIssue.IssueType.API_SCORE_CHANGED.name();

        Optional<ApiSyncIssue> existingIssue = apiSyncIssueRepository.findFirstByProviderAndIssueTypeAndGameResultId(
                provider,
                issueType,
                existing.getId()
        );
        if (existingIssue.isPresent()) {
            ApiSyncIssue issue = existingIssue.get();
            issue.setCreatedAt(LocalDateTime.now());
            issue.setMessage(message);
            issue.setMatchday(existing.getMatchday());
            issue.setExternalMatchId(incomingSource != null ? incomingSource.getExternalMatchId() : null);
            apiSyncIssueRepository.save(issue);
            return;
        }

        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(provider)
                .issueType(issueType)
                .leagueCode(existing.getLeagueCode())
                .season(existing.getSeason())
                .matchday(existing.getMatchday())
                .gameResultId(existing.getId())
                .externalMatchId(incomingSource != null ? incomingSource.getExternalMatchId() : null)
                .homeTeamName(sideName(incomingSource, true))
                .awayTeamName(sideName(incomingSource, false))
                .homeTeamExternalId(sideExternalId(incomingSource, true))
                .awayTeamExternalId(sideExternalId(incomingSource, false))
                .message(message)
                .build());
    }

    private static String buildApiScoreChangedMessage(
            GameScore storedScore,
            GameScore incomingScore,
            boolean finalized,
            LocalDateTime incomingApiLastUpdated
    ) {
        return "stored="
                + GameScoreValidator.formatDisplay(storedScore)
                + " api="
                + GameScoreValidator.formatDisplay(incomingScore)
                + " apiLastUpdated="
                + incomingApiLastUpdated
                + (finalized ? " finalized" : "");
    }

    private static String sideExternalId(GameResultSourceSnapshot source, boolean home) {
        if (source == null) {
            return null;
        }
        GameResultSideSnapshot side = home ? source.getHome() : source.getAway();
        return side != null ? side.getExternalId() : null;
    }

    public void recordInvalidCanonicalScore(GameResultRecord record) {
        recordMatchIssue(record, ApiSyncIssue.IssueType.INVALID_CANONICAL_SCORE,
                "score=" + GameScoreValidator.formatFullDisplay(record.getGameScore()));
    }

    public void recordScoreNotStable(GameResultRecord record) {
        recordMatchIssue(record, ApiSyncIssue.IssueType.SCORE_NOT_STABLE,
                "stablePolls=" + record.getStableScorePollCount()
                        + " score=" + GameScoreValidator.formatFullDisplay(record.getGameScore()));
    }

    public void recordProviderScoreMismatch(GameResultRecord record) {
        GameScore primary = scoreFromSource(record.footballDataSource());
        GameScore secondary = scoreFromSource(record.apiFootballSource());
        recordMatchIssue(record, ApiSyncIssue.IssueType.PROVIDER_SCORE_MISMATCH,
                "primary=" + GameScoreValidator.formatFullDisplay(primary)
                        + " secondary=" + GameScoreValidator.formatFullDisplay(secondary));
    }

    public void recordPrimaryProviderUnavailable(GameResultRecord record) {
        recordMatchIssue(record, ApiSyncIssue.IssueType.PRIMARY_PROVIDER_UNAVAILABLE, "primary missing");
    }

    public void recordSecondaryProviderUnavailable(GameResultRecord record) {
        recordMatchIssue(record, ApiSyncIssue.IssueType.SECONDARY_PROVIDER_UNAVAILABLE, "secondary missing");
    }

    private void recordMatchIssue(GameResultRecord record, ApiSyncIssue.IssueType issueType, String message) {
        if (record == null || record.getId() == null) {
            return;
        }
        String provider = MatchDataProviders.FOOTBALL_DATA;
        String type = issueType.name();
        Optional<ApiSyncIssue> existingIssue = apiSyncIssueRepository.findFirstByProviderAndIssueTypeAndGameResultId(
                provider, type, record.getId());
        GameResultSourceSnapshot source = record.footballDataSource();
        if (existingIssue.isPresent()) {
            ApiSyncIssue issue = existingIssue.get();
            issue.setCreatedAt(LocalDateTime.now());
            issue.setMessage(message);
            apiSyncIssueRepository.save(issue);
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(provider)
                .issueType(type)
                .leagueCode(record.getLeagueCode())
                .season(record.getSeason())
                .matchday(record.getMatchday())
                .gameResultId(record.getId())
                .externalMatchId(source != null ? source.getExternalMatchId() : null)
                .homeTeamName(sideName(source, true))
                .awayTeamName(sideName(source, false))
                .message(message)
                .build());
    }

    private static GameScore scoreFromSource(GameResultSourceSnapshot source) {
        return source != null ? source.getGameScore() : null;
    }

    public List<ApiSyncIssue> getLatest() {
        purgeResolvedTeamMappingIssues();
        return apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc();
    }

    public void deleteById(String id) {
        if (id == null || id.isBlank() || !apiSyncIssueRepository.existsById(id)) {
            throw new NotFoundException("ApiSyncIssue", id);
        }
        apiSyncIssueRepository.deleteById(id);
    }

    /**
     * Removes {@code TEAM_MAPPING_MISSING} entries for one external team once its alias is saved.
     */
    public int purgeTeamMappingIssuesForExternalTeam(
            String provider,
            String externalName,
            Integer externalId
    ) {
        if (!isExternalTeamMapped(provider, externalId, externalName)) {
            return 0;
        }
        List<String> toDelete = collectMatchingTeamMappingIssueIds(provider, externalName, externalId);
        if (toDelete.isEmpty()) {
            return 0;
        }
        apiSyncIssueRepository.deleteAllById(toDelete);
        return toDelete.size();
    }

    /** Drops resolved team-mapping issues so refresh reflects current aliases. */
    public int purgeResolvedTeamMappingIssues() {
        List<ApiSyncIssue> all = apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc();
        List<String> toDelete = new ArrayList<>();
        for (ApiSyncIssue issue : all) {
            if (isResolvedTeamMappingIssue(issue)) {
                toDelete.add(issue.getId());
            }
        }
        if (toDelete.isEmpty()) {
            return 0;
        }
        apiSyncIssueRepository.deleteAllById(toDelete);
        return toDelete.size();
    }

    public void clearAll() {
        apiSyncIssueRepository.deleteAll();
    }

    public boolean hasIssues() {
        return apiSyncIssueRepository.count() > 0;
    }

    public boolean hasScoreChangeIssues() {
        return apiSyncIssueRepository.existsByIssueType(ApiSyncIssue.IssueType.API_SCORE_CHANGED.name());
    }

    public boolean hasOddsMappingIssues() {
        return apiSyncIssueRepository.existsByIssueTypeIn(List.of(
                ApiSyncIssue.IssueType.ODDS_MARKET_UNMAPPED.name(),
                ApiSyncIssue.IssueType.ODDS_SELECTION_UNMAPPED.name(),
                ApiSyncIssue.IssueType.ODDS_QUOTE_MISMATCH.name(),
                ApiSyncIssue.IssueType.ODDS_QUOTE_REJECTED.name()
        ));
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
            if (teamAliasResolver.oddsApiAliasesMapped(parsedId > 0 ? parsedId : null, name)) {
                return;
            }
        } else if (MatchDataProviders.API_FOOTBALL.equals(provider)) {
            if (teamAliasResolver.resolveApiFootball(parsedId > 0 ? parsedId : null, name).isPresent()) {
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

    private boolean isResolvedTeamMappingIssue(ApiSyncIssue issue) {
        if (issue == null || !ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
            return false;
        }
        String provider = issue.getProvider() != null
                ? issue.getProvider()
                : MatchDataProviders.FOOTBALL_DATA;
        if (issue.getHomeTeamName() != null || issue.getHomeTeamExternalId() != null) {
            return isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getHomeTeamExternalId()),
                    issue.getHomeTeamName()
            );
        }
        if (issue.getAwayTeamName() != null || issue.getAwayTeamExternalId() != null) {
            return isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getAwayTeamExternalId()),
                    issue.getAwayTeamName()
            );
        }
        return false;
    }

    private List<String> collectMatchingTeamMappingIssueIds(
            String provider,
            String externalName,
            Integer externalId
    ) {
        List<String> toDelete = new ArrayList<>();
        for (ApiSyncIssue issue : apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (!providerEquals(issue.getProvider(), provider)) {
                continue;
            }
            if (issueMatchesExternalTeam(issue, externalName, externalId)) {
                toDelete.add(issue.getId());
            }
        }
        return toDelete;
    }

    private static boolean providerEquals(String issueProvider, String provider) {
        String left = issueProvider != null ? issueProvider : MatchDataProviders.FOOTBALL_DATA;
        String right = provider != null ? provider : MatchDataProviders.FOOTBALL_DATA;
        return left.equals(right);
    }

    private static boolean issueMatchesExternalTeam(
            ApiSyncIssue issue,
            String externalName,
            Integer externalId
    ) {
        return matchesSide(externalName, externalId, issue.getHomeTeamName(), issue.getHomeTeamExternalId())
                || matchesSide(externalName, externalId, issue.getAwayTeamName(), issue.getAwayTeamExternalId());
    }

    private static boolean matchesSide(
            String externalName,
            Integer externalId,
            String issueName,
            String issueExternalId
    ) {
        if (externalId != null && externalId > 0 && issueExternalId != null
                && String.valueOf(externalId).equals(issueExternalId.trim())) {
            return true;
        }
        return externalName != null && !externalName.isBlank() && externalName.equals(issueName);
    }

    private boolean isExternalTeamMapped(String provider, Integer externalId, String externalName) {
        if (MatchDataProviders.ODDS_API.equals(provider)) {
            return teamAliasResolver.oddsApiAliasesMapped(externalId, externalName);
        }
        if (MatchDataProviders.API_FOOTBALL.equals(provider)) {
            return teamAliasResolver.resolveApiFootball(externalId, externalName).isPresent();
        }
        int footballDataId = externalId != null && externalId > 0 ? externalId : 0;
        return teamAliasResolver.resolveFootballData(footballDataId, externalName).isPresent();
    }
}
