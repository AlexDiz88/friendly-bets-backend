package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.fourscore.FourScoreListMatch;
import net.friendly_bets.gameresults.GameScoreValidator;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultStabilizationService;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.TeamsRepository;
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
    private final GameResultRecordRepository gameResultRecordRepository;
    private final TeamsRepository teamsRepository;
    private final TeamAliasResolver teamAliasResolver;
    private final MatchResultSyncSettingsService matchResultSyncSettingsService;
    private final MatchResultStabilizationService stabilizationService;

    public void recordMissingFourScoreTeamMapping(
            String leagueCode,
            String season,
            int matchday,
            Long externalEventId,
            boolean home,
            String teamName
    ) {
        if (teamName == null || teamName.isBlank()) {
            return;
        }
        if (teamAliasResolver.resolveFourScoreByName(teamName).isPresent()) {
            return;
        }
        if (hasUnresolvedFourScoreTeamMappingIssue(teamName)) {
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.FOURSCORE)
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .externalMatchId(externalEventId)
                .homeTeamName(home ? teamName : null)
                .awayTeamName(home ? null : teamName)
                .build());
    }

    public void recordMissingTwentyFourScoreTeamMapping(
            String leagueCode,
            String season,
            int matchday,
            Long externalMatchId,
            boolean home,
            String teamName
    ) {
        if (teamName == null || teamName.isBlank()) {
            return;
        }
        if (teamAliasResolver.resolveTwentyFourScoreByName(teamName).isPresent()) {
            return;
        }
        if (hasUnresolvedTwentyFourScoreTeamMappingIssue(teamName)) {
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.TWENTYFOUR_SCORE)
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .externalMatchId(externalMatchId)
                .homeTeamName(home ? teamName : null)
                .awayTeamName(home ? null : teamName)
                .build());
    }

    private boolean hasUnresolvedTwentyFourScoreTeamMappingIssue(String teamName) {
        for (ApiSyncIssue issue : apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (!providerEquals(issue.getProvider(), MatchDataProviders.TWENTYFOUR_SCORE)) {
                continue;
            }
            if (!issueMatchesExternalName(issue, teamName)) {
                continue;
            }
            return !isResolvedTeamMappingIssue(issue);
        }
        return false;
    }

    public void recordFourScoreEventMappingMissing(
            FourScoreListMatch listMatch,
            String leagueCode,
            String season,
            int matchday
    ) {
        if (listMatch == null) {
            return;
        }
        Long externalEventId = listMatch.getExternalEventId();
        if (externalEventId != null
                && apiSyncIssueRepository.existsByProviderAndIssueTypeAndExternalMatchId(
                MatchDataProviders.FOURSCORE,
                ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name(),
                externalEventId)) {
            return;
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.FOURSCORE)
                .issueType(ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .externalMatchId(listMatch.getExternalEventId())
                .homeTeamName(listMatch.getHomeTeamName())
                .awayTeamName(listMatch.getAwayTeamName())
                .message("fourScoreGameResultNotFound")
                .build());
    }

    private boolean hasUnresolvedFourScoreTeamMappingIssue(String teamName) {
        for (ApiSyncIssue issue : apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (!providerEquals(issue.getProvider(), MatchDataProviders.FOURSCORE)) {
                continue;
            }
            if (!issueMatchesExternalName(issue, teamName)) {
                continue;
            }
            return !isResolvedTeamMappingIssue(issue);
        }
        return false;
    }

    private static boolean issueMatchesExternalName(ApiSyncIssue issue, String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return false;
        }
        return teamName.equals(issue.getHomeTeamName()) || teamName.equals(issue.getAwayTeamName());
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
        GameResultSourceSnapshot source = match.primaryExternalSource();
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.ODDS_API)
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
        GameResultSourceSnapshot source = match != null ? match.primaryExternalSource() : null;
        String provider = MatchDataProviders.ODDS_API;
        String type = issueType.name();
        String gameResultId = match != null ? match.getId() : null;
        if (gameResultId != null && !gameResultId.isBlank()) {
            Optional<ApiSyncIssue> existingIssue = apiSyncIssueRepository
                    .findFirstByProviderAndIssueTypeAndGameResultIdAndMessage(
                            provider, type, gameResultId, message);
            if (existingIssue.isPresent()) {
                ApiSyncIssue issue = existingIssue.get();
                issue.setCreatedAt(LocalDateTime.now());
                issue.setMatchday(matchday);
                issue.setLeagueCode(leagueCode);
                issue.setSeason(season);
                apiSyncIssueRepository.save(issue);
                return;
            }
        }
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.ODDS_API)
                .issueType(issueType.name())
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .gameResultId(match != null ? match.getId() : null)
                .externalMatchId(source != null ? source.getExternalMatchId() : null)
                .homeTeamName(resolveTeamName(match, source, true))
                .awayTeamName(resolveTeamName(match, source, false))
                .message(withOddsApiEventId(match, message))
                .build());
    }

    private static String withOddsApiEventId(GameResultRecord match, String message) {
        if (match == null || match.getOddsApiEventId() == null || match.getOddsApiEventId() <= 0) {
            return message;
        }
        return "oddsApiEventId=" + match.getOddsApiEventId()
                + (message != null && !message.isBlank() ? " · " + message : "");
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
     * Внешний провайдер обновил матч ({@code lastUpdated} новее сохранённого), а счёт отличается от канона.
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

        GameResultSourceSnapshot storedSource = existing.primaryExternalSource();
        GameResultSourceSnapshot incomingSource = incoming.primaryExternalSource();
        LocalDateTime storedApiLastUpdated = storedSource != null ? storedSource.getApiLastUpdated() : null;
        LocalDateTime incomingApiLastUpdated = incomingSource != null ? incomingSource.getApiLastUpdated() : null;
        if (incomingApiLastUpdated == null) {
            return;
        }
        if (storedApiLastUpdated != null && !incomingApiLastUpdated.isAfter(storedApiLastUpdated)) {
            return;
        }

        String message = buildApiScoreChangedMessage(storedScore, incomingScore, existing.isFinalized(), incomingApiLastUpdated);
        String provider = MatchDataProviders.FOURSCORE;
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
            applyTeamNamesToIssue(issue, existing, incomingSource);
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
                .homeTeamName(resolveTeamName(existing, incomingSource, true))
                .awayTeamName(resolveTeamName(existing, incomingSource, false))
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
        String reason = stabilizationService.describeStabilityBlock(record);
        var settings = matchResultSyncSettingsService.getEffective();
        if (settings.isDualVerificationEnabled()) {
            GameResultSourceSnapshot secondary = record.sourceFor(
                    MatchDataProviders.sourcesStorageKey(settings.getSecondaryProvider()));
            if (secondary != null) {
                reason += " secondaryStablePolls=" + secondary.getStableScorePollCount();
            }
        }
        recordMatchIssue(record, ApiSyncIssue.IssueType.SCORE_NOT_STABLE,
                reason + " score=" + GameScoreValidator.formatFullDisplay(record.getGameScore()));
    }

    public void recordProviderScoreMismatch(GameResultRecord record) {
        var settings = matchResultSyncSettingsService.getEffective();
        GameScore primary = record.getGameScore();
        GameResultSourceSnapshot secondarySource = record.sourceFor(
                MatchDataProviders.sourcesStorageKey(settings.getSecondaryProvider()));
        GameScore secondary = scoreFromSource(secondarySource);
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
        String provider = record.getProvider() != null ? record.getProvider() : MatchDataProviders.FOURSCORE;
        String type = issueType.name();
        Optional<ApiSyncIssue> existingIssue = apiSyncIssueRepository.findFirstByProviderAndIssueTypeAndGameResultId(
                provider, type, record.getId());
        GameResultSourceSnapshot source = sourceForProvider(record, provider);
        if (existingIssue.isPresent()) {
            ApiSyncIssue issue = existingIssue.get();
            issue.setCreatedAt(LocalDateTime.now());
            issue.setMessage(message);
            applyTeamNamesToIssue(issue, record, source);
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
                .homeTeamName(resolveTeamName(record, source, true))
                .awayTeamName(resolveTeamName(record, source, false))
                .message(message)
                .build());
    }

    private static GameScore scoreFromSource(GameResultSourceSnapshot source) {
        return source != null ? source.getGameScore() : null;
    }

    private static GameResultSourceSnapshot sourceForProvider(GameResultRecord record, String provider) {
        if (record == null || provider == null) {
            return null;
        }
        GameResultSourceSnapshot source = record.sourceFor(MatchDataProviders.sourcesStorageKey(provider));
        return source != null ? source : record.primaryExternalSource();
    }

    public List<ApiSyncIssue> getLatest() {
        purgeResolvedTeamMappingIssues();
        List<ApiSyncIssue> issues = dedupeOddsMappingIssues(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc());
        for (ApiSyncIssue issue : issues) {
            enrichTeamNamesIfMissing(issue);
        }
        return issues;
    }

    /** Одна и та же odds-проблема не дублируется — обновляется дата последнего появления. */
    private static List<ApiSyncIssue> dedupeOddsMappingIssues(List<ApiSyncIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        Map<String, ApiSyncIssue> byKey = new LinkedHashMap<>();
        for (ApiSyncIssue issue : issues) {
            if (issue == null) {
                continue;
            }
            if (!isDedupableOddsIssue(issue)) {
                byKey.put("other:" + issue.getId(), issue);
                continue;
            }
            String key = oddsIssueDedupKey(issue);
            ApiSyncIssue existing = byKey.get(key);
            if (existing == null || isNewer(issue, existing)) {
                byKey.put(key, issue);
            }
        }
        List<ApiSyncIssue> result = new ArrayList<>(byKey.values());
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null) {
                return 1;
            }
            if (b.getCreatedAt() == null) {
                return -1;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return result;
    }

    private static boolean isDedupableOddsIssue(ApiSyncIssue issue) {
        if (!MatchDataProviders.ODDS_API.equals(issue.getProvider())) {
            return false;
        }
        String type = issue.getIssueType();
        return ApiSyncIssue.IssueType.ODDS_MARKET_UNMAPPED.name().equals(type)
                || ApiSyncIssue.IssueType.ODDS_SELECTION_UNMAPPED.name().equals(type)
                || ApiSyncIssue.IssueType.ODDS_QUOTE_MISMATCH.name().equals(type)
                || ApiSyncIssue.IssueType.ODDS_QUOTE_REJECTED.name().equals(type);
    }

    private static String oddsIssueDedupKey(ApiSyncIssue issue) {
        return issue.getProvider() + "\0"
                + issue.getIssueType() + "\0"
                + (issue.getGameResultId() != null ? issue.getGameResultId() : "") + "\0"
                + (issue.getMessage() != null ? issue.getMessage() : "");
    }

    private static boolean isNewer(ApiSyncIssue candidate, ApiSyncIssue existing) {
        if (candidate.getCreatedAt() == null) {
            return false;
        }
        if (existing.getCreatedAt() == null) {
            return true;
        }
        return candidate.getCreatedAt().isAfter(existing.getCreatedAt());
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
        List<String> toDelete = new ArrayList<>();
        List<ApiSyncIssue> toSave = new ArrayList<>();
        collectTeamMappingIssueUpdates(provider, externalName, externalId, toDelete, toSave);
        if (toDelete.isEmpty() && toSave.isEmpty()) {
            return 0;
        }
        if (!toSave.isEmpty()) {
            apiSyncIssueRepository.saveAll(toSave);
        }
        if (!toDelete.isEmpty()) {
            apiSyncIssueRepository.deleteAllById(toDelete);
        }
        return toDelete.size() + toSave.size();
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
                    : MatchDataProviders.FOURSCORE;
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
        } else if (MatchDataProviders.FOURSCORE.equals(provider)) {
            if (teamAliasResolver.resolveFourScoreByName(name).isPresent()) {
                return;
            }
        } else if (MatchDataProviders.TWENTYFOUR_SCORE.equals(provider)) {
            if (teamAliasResolver.resolveTwentyFourScoreByName(name).isPresent()) {
                return;
            }
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

    private String resolveTeamName(GameResultRecord record, GameResultSourceSnapshot source, boolean home) {
        String externalName = sideName(source, home);
        if (externalName != null && !externalName.isBlank()) {
            return externalName;
        }
        return internalTeamTitle(record, home);
    }

    private String internalTeamTitle(GameResultRecord record, boolean home) {
        if (record == null) {
            return null;
        }
        String teamId = home ? record.getHomeTeamId() : record.getAwayTeamId();
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return teamsRepository.findById(teamId)
                .map(Team::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .orElse(null);
    }

    private void applyTeamNamesToIssue(
            ApiSyncIssue issue,
            GameResultRecord record,
            GameResultSourceSnapshot source
    ) {
        if (issue == null || record == null) {
            return;
        }
        if (issue.getHomeTeamName() == null || issue.getHomeTeamName().isBlank()) {
            issue.setHomeTeamName(resolveTeamName(record, source, true));
        }
        if (issue.getAwayTeamName() == null || issue.getAwayTeamName().isBlank()) {
            issue.setAwayTeamName(resolveTeamName(record, source, false));
        }
    }

    private void enrichTeamNamesIfMissing(ApiSyncIssue issue) {
        if (issue == null || issue.getGameResultId() == null || issue.getGameResultId().isBlank()) {
            return;
        }
        boolean needsHome = issue.getHomeTeamName() == null || issue.getHomeTeamName().isBlank();
        boolean needsAway = issue.getAwayTeamName() == null || issue.getAwayTeamName().isBlank();
        if (!needsHome && !needsAway) {
            return;
        }
        gameResultRecordRepository.findById(issue.getGameResultId()).ifPresent(record -> {
            GameResultSourceSnapshot source = sourceForProvider(record, issue.getProvider());
            if (needsHome) {
                issue.setHomeTeamName(resolveTeamName(record, source, true));
            }
            if (needsAway) {
                issue.setAwayTeamName(resolveTeamName(record, source, false));
            }
        });
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
                : MatchDataProviders.FOURSCORE;
        boolean hasHome = issue.getHomeTeamName() != null || issue.getHomeTeamExternalId() != null;
        boolean hasAway = issue.getAwayTeamName() != null || issue.getAwayTeamExternalId() != null;
        if (hasHome && hasAway) {
            return isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getHomeTeamExternalId()),
                    issue.getHomeTeamName()
            ) && isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getAwayTeamExternalId()),
                    issue.getAwayTeamName()
            );
        }
        if (hasHome) {
            return isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getHomeTeamExternalId()),
                    issue.getHomeTeamName()
            );
        }
        if (hasAway) {
            return isExternalTeamMapped(
                    provider,
                    parseExternalId(issue.getAwayTeamExternalId()),
                    issue.getAwayTeamName()
            );
        }
        return false;
    }

    private void collectTeamMappingIssueUpdates(
            String provider,
            String externalName,
            Integer externalId,
            List<String> toDelete,
            List<ApiSyncIssue> toSave
    ) {
        for (ApiSyncIssue issue : apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (!providerEquals(issue.getProvider(), provider)) {
                continue;
            }
            if (!issueMatchesExternalTeam(issue, externalName, externalId)) {
                continue;
            }
            boolean homeMatches = matchesSide(
                    externalName,
                    externalId,
                    issue.getHomeTeamName(),
                    issue.getHomeTeamExternalId()
            );
            boolean awayMatches = matchesSide(
                    externalName,
                    externalId,
                    issue.getAwayTeamName(),
                    issue.getAwayTeamExternalId()
            );
            boolean hasHome = issue.getHomeTeamName() != null || issue.getHomeTeamExternalId() != null;
            boolean hasAway = issue.getAwayTeamName() != null || issue.getAwayTeamExternalId() != null;
            if (hasHome && hasAway && (homeMatches ^ awayMatches)) {
                if (homeMatches) {
                    issue.setHomeTeamName(null);
                    issue.setHomeTeamExternalId(null);
                } else {
                    issue.setAwayTeamName(null);
                    issue.setAwayTeamExternalId(null);
                }
                toSave.add(issue);
            } else {
                toDelete.add(issue.getId());
            }
        }
    }

    private static boolean providerEquals(String issueProvider, String provider) {
        String left = issueProvider != null ? issueProvider : MatchDataProviders.FOURSCORE;
        String right = provider != null ? provider : MatchDataProviders.FOURSCORE;
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
                MatchDataProviders.MARATHONBET,
                ApiSyncIssue.IssueType.EVENT_MAPPING_MISSING.name(),
                match.getId())) {
            return;
        }
        GameResultSourceSnapshot source = match.primaryExternalSource();
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.MARATHONBET)
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
                .provider(MatchDataProviders.MARATHONBET)
                .issueType(ApiSyncIssue.IssueType.MARATHONBET_FETCH_FAILED.name())
                .leagueCode(leagueCode)
                .season(season)
                .message(message)
                .build());
    }

    public void recordMarathonbetPrimaryUnavailable(String leagueCode, String season, String message) {
        apiSyncIssueRepository.save(ApiSyncIssue.builder()
                .createdAt(LocalDateTime.now())
                .provider(MatchDataProviders.MARATHONBET)
                .issueType(ApiSyncIssue.IssueType.PRIMARY_PROVIDER_UNAVAILABLE.name())
                .leagueCode(leagueCode)
                .season(season)
                .message(message)
                .build());
    }

    private boolean isExternalTeamMapped(String provider, Integer externalId, String externalName) {
        if (MatchDataProviders.MARATHONBET.equals(provider)) {
            return teamAliasResolver.resolveMarathonbetByName(externalName).isPresent();
        }
        if (MatchDataProviders.FOURSCORE.equals(provider)) {
            return teamAliasResolver.resolveFourScoreByName(externalName).isPresent();
        }
        if (MatchDataProviders.TWENTYFOUR_SCORE.equals(provider)) {
            return teamAliasResolver.resolveTwentyFourScoreByName(externalName).isPresent();
        }
        if (MatchDataProviders.ODDS_API.equals(provider)) {
            return teamAliasResolver.oddsApiAliasesMapped(externalId, externalName);
        }
        return false;
    }
}
