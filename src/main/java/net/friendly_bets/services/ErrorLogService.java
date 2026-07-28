package net.friendly_bets.services;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.friendly_bets.dto.ErrorLogDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.ErrorLog;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Persists operator-visible sync/provider errors to {@code error_logs}.
 * Never throws to callers — logging must not break sync pipelines.
 */
@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);

    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARN = "WARN";
    public static final String ROLE_PRIMARY = "PRIMARY";
    public static final String ROLE_SECONDARY = "SECONDARY";

    public static final String CODE_TEAM_MAPPING_MISSING = "teamMappingMissing";
    public static final String CODE_TEAM_ALIAS_MISMATCH = "teamAliasMismatch";
    public static final String CODE_EVENT_MAPPING_MISSING = "eventMappingMissing";
    public static final String CODE_PROVIDER_FETCH_FAILED = "providerFetchFailed";
    public static final String CODE_PRIMARY_UNAVAILABLE = "primaryProviderUnavailable";
    public static final String CODE_SECONDARY_UNAVAILABLE = "secondaryProviderUnavailable";
    public static final String CODE_LAYER_FAILED = "layerProviderFailed";
    public static final String CODE_FULL_MATCH_FAILED = "fullMatchFailed";

    private final ErrorLogRepository errorLogRepository;

    @Value
    @Builder
    public static class Entry {
        String severity;
        String layer;
        String provider;
        String providerRole;
        String code;
        String message;
        String leagueCode;
        String season;
        Integer matchday;
        String matchScheduleId;
        String externalMatchId;
        String homeTeam;
        String awayTeam;
        @Builder.Default
        Map<String, String> context = new LinkedHashMap<>();
        /** Skip insert if same provider+code+matchScheduleId already exists. */
        boolean dedupeByMatch;
    }

    public void record(Entry entry) {
        if (entry == null || entry.getCode() == null || entry.getCode().isBlank()) {
            return;
        }
        try {
            if (entry.isDedupeByMatch()
                    && entry.getMatchScheduleId() != null
                    && !entry.getMatchScheduleId().isBlank()
                    && errorLogRepository.existsByProviderAndCodeAndMatchScheduleId(
                    blankToNull(entry.getProvider()),
                    entry.getCode().trim(),
                    entry.getMatchScheduleId().trim())) {
                return;
            }
            ErrorLog doc = ErrorLog.builder()
                    .createdAt(Instant.now())
                    .severity(blankToNull(entry.getSeverity()) != null ? entry.getSeverity().trim().toUpperCase(Locale.ROOT) : SEVERITY_ERROR)
                    .layer(blankToNull(entry.getLayer()))
                    .provider(blankToNull(entry.getProvider()))
                    .providerRole(blankToNull(entry.getProviderRole()))
                    .code(entry.getCode().trim())
                    .message(blankToNull(entry.getMessage()))
                    .leagueCode(blankToNull(entry.getLeagueCode()))
                    .season(blankToNull(entry.getSeason()))
                    .matchday(entry.getMatchday())
                    .matchScheduleId(blankToNull(entry.getMatchScheduleId()))
                    .externalMatchId(blankToNull(entry.getExternalMatchId()))
                    .homeTeam(blankToNull(entry.getHomeTeam()))
                    .awayTeam(blankToNull(entry.getAwayTeam()))
                    .context(entry.getContext() != null ? new LinkedHashMap<>(entry.getContext()) : new LinkedHashMap<>())
                    .build();
            errorLogRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to persist error_log: {}", e.getMessage());
        }
    }

    public void recordLayerFailure(
            ExternalDataLayer layer,
            String providerId,
            String providerRole,
            String code,
            String message,
            String leagueCode
    ) {
        record(Entry.builder()
                .severity(SEVERITY_ERROR)
                .layer(layer != null ? layer.name() : null)
                .provider(providerId)
                .providerRole(providerRole)
                .code(code != null ? code : CODE_LAYER_FAILED)
                .message(message)
                .leagueCode(leagueCode)
                .build());
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
        record(Entry.builder()
                .severity(SEVERITY_WARN)
                .layer(ExternalDataLayer.SCHEDULE.name())
                .provider(provider)
                .code(CODE_TEAM_MAPPING_MISSING)
                .message(message)
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .homeTeam(homeTeamName)
                .awayTeam(awayTeamName)
                .build());
    }

    public void recordEventMappingMissing(MatchSchedule match, String provider, String leagueCode, String season, int matchday, String message) {
        if (match == null || match.getId() == null) {
            return;
        }
        record(Entry.builder()
                .severity(SEVERITY_WARN)
                .layer(ExternalDataLayer.ODDS.name())
                .provider(provider)
                .code(CODE_EVENT_MAPPING_MISSING)
                .message(message)
                .leagueCode(leagueCode)
                .season(season)
                .matchday(matchday)
                .matchScheduleId(match.getId())
                .homeTeam(null)
                .awayTeam(null)
                .dedupeByMatch(true)
                .build());
    }

    public void recordFullMatchFailure(MatchSchedule match, String provider, String message) {
        record(Entry.builder()
                .severity(SEVERITY_ERROR)
                .layer(ExternalDataLayer.FULL_MATCH.name())
                .provider(provider)
                .code(CODE_FULL_MATCH_FAILED)
                .message(message)
                .leagueCode(match != null ? match.getLeagueCode() : null)
                .matchday(match != null ? match.getMatchday() : null)
                .matchScheduleId(match != null ? match.getId() : null)
                .externalMatchId(null)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ErrorLogDto> listRecent() {
        return ErrorLogDto.fromList(errorLogRepository.findTop200ByOrderByCreatedAtDesc());
    }

    public long count() {
        return errorLogRepository.count();
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new NotFoundException("ErrorLog", id);
        }
        if (!errorLogRepository.existsById(id)) {
            throw new NotFoundException("ErrorLog", id);
        }
        errorLogRepository.deleteById(id);
    }

    @Transactional
    public long clearAll() {
        long count = errorLogRepository.count();
        errorLogRepository.deleteAll();
        return count;
    }

    @Transactional
    public int purgeTeamMappingIssuesForExternalTeam(String provider, String externalName) {
        if (externalName == null || externalName.isBlank()) {
            return 0;
        }
        String name = externalName.trim();
        List<ErrorLog> toDelete = new ArrayList<>();
        for (ErrorLog issue : errorLogRepository.findByCodeAndHomeTeam(CODE_TEAM_MAPPING_MISSING, name)) {
            if (provider == null || provider.equals(issue.getProvider())) {
                toDelete.add(issue);
            }
        }
        for (ErrorLog issue : errorLogRepository.findByCodeAndAwayTeam(CODE_TEAM_MAPPING_MISSING, name)) {
            if (provider == null || provider.equals(issue.getProvider())) {
                toDelete.add(issue);
            }
        }
        if (!toDelete.isEmpty()) {
            errorLogRepository.deleteAll(toDelete);
        }
        return toDelete.size();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
