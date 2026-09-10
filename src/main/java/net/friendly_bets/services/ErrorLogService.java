package net.friendly_bets.services;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.friendly_bets.dto.ErrorLogDto;
import net.friendly_bets.dto.ExternalApiMatchTeamsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.ErrorLog;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.ErrorLogRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.TeamsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public static final String CODE_FULL_MATCH_NOT_READY = "fullMatchNotReady";
    public static final String CODE_LIVE_MATCH_SYNC_STUCK = "liveMatchSyncStuck";
    public static final String CODE_LIVE_MATCH_NOT_IN_FEED = "liveMatchNotInFeed";
    public static final String CODE_LIVE_MATCH_AMBIGUOUS = "liveMatchAmbiguous";
    public static final String CODE_LIVE_MATCH_NEVER_POLLED = "liveMatchNeverPolled";

    private final ErrorLogRepository errorLogRepository;
    private final MatchScheduleRepository matchScheduleRepository;
    private final TeamsRepository teamsRepository;

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
        /** If same provider+code+matchScheduleId exists, append Instant instead of inserting. */
        boolean dedupeByMatch;
    }

    public void record(Entry entry) {
        if (entry == null || entry.getCode() == null || entry.getCode().isBlank()) {
            return;
        }
        try {
            ResolvedEntry resolved = resolveEntry(entry);
            if (entry.isDedupeByMatch()
                    && resolved.matchScheduleId != null) {
                Optional<ErrorLog> existing = errorLogRepository.findFirstByProviderAndCodeAndMatchScheduleId(
                        resolved.provider,
                        entry.getCode().trim(),
                        resolved.matchScheduleId);
                if (existing.isPresent()) {
                    ErrorLog doc = existing.get();
                    appendOccurrence(doc, Instant.now());
                    errorLogRepository.save(doc);
                    return;
                }
            }
            Instant now = Instant.now();
            List<Instant> occurredAt = new ArrayList<>();
            occurredAt.add(now);
            ErrorLog doc = ErrorLog.builder()
                    .createdAt(now)
                    .firstOccurredAt(now)
                    .lastOccurredAt(now)
                    .occurredAt(occurredAt)
                    .occurrenceCount(1)
                    .severity(blankToNull(entry.getSeverity()) != null ? entry.getSeverity().trim().toUpperCase(Locale.ROOT) : SEVERITY_ERROR)
                    .layer(resolved.layer)
                    .provider(resolved.provider)
                    .providerRole(blankToNull(entry.getProviderRole()))
                    .code(entry.getCode().trim())
                    .message(resolved.message)
                    .leagueCode(resolved.leagueCode)
                    .season(resolved.season)
                    .matchday(resolved.matchday)
                    .matchScheduleId(resolved.matchScheduleId)
                    .externalMatchId(blankToNull(entry.getExternalMatchId()))
                    .homeTeam(resolved.homeTeam)
                    .awayTeam(resolved.awayTeam)
                    .context(entry.getContext() != null ? new LinkedHashMap<>(entry.getContext()) : new LinkedHashMap<>())
                    .build();
            errorLogRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to persist error_log: {}", e.getMessage());
        }
    }

    /**
     * Builds the stored message: original detail plus always layer / provider / league / matchday / teams
     * (explicit «недоступн*» when a field is missing).
     */
    String toStoredMessage(Entry entry) {
        return resolveEntry(entry).message;
    }

    private ResolvedEntry resolveEntry(Entry entry) {
        String layer = blankToNull(entry.getLayer());
        String provider = blankToNull(entry.getProvider());
        String leagueCode = blankToNull(entry.getLeagueCode());
        String season = blankToNull(entry.getSeason());
        Integer matchday = entry.getMatchday();
        String matchScheduleId = blankToNull(entry.getMatchScheduleId());
        String homeTeam = blankToNull(entry.getHomeTeam());
        String awayTeam = blankToNull(entry.getAwayTeam());

        if (matchScheduleId != null && matchScheduleRepository != null) {
            MatchSchedule schedule = matchScheduleRepository.findById(matchScheduleId).orElse(null);
            if (schedule != null) {
                if (leagueCode == null) {
                    leagueCode = blankToNull(schedule.getLeagueCode());
                }
                if (season == null) {
                    season = blankToNull(schedule.getSeasonId());
                }
                if (matchday == null) {
                    matchday = schedule.getMatchday();
                }
                if (homeTeam == null) {
                    homeTeam = resolveTeamTitle(schedule.getHomeTeamId());
                }
                if (awayTeam == null) {
                    awayTeam = resolveTeamTitle(schedule.getAwayTeamId());
                }
            }
        }

        String message = formatInformativeMessage(
                blankToNull(entry.getMessage()),
                layer,
                provider,
                leagueCode,
                matchday,
                homeTeam,
                awayTeam
        );
        return new ResolvedEntry(layer, provider, leagueCode, season, matchday, matchScheduleId, homeTeam, awayTeam, message);
    }

    private String resolveTeamTitle(String teamId) {
        if (teamId == null || teamId.isBlank() || teamsRepository == null) {
            return blankToNull(teamId);
        }
        return teamsRepository.findById(teamId.trim())
                .map(Team::getTitle)
                .map(ErrorLogService::blankToNull)
                .orElse(teamId.trim());
    }

    private static final String CONTEXT_SEP = " | ";

    static String formatInformativeMessage(
            String baseMessage,
            String layer,
            String provider,
            String leagueCode,
            Integer matchday,
            String homeTeam,
            String awayTeam
    ) {
        String suffix = buildContextSuffix(layer, provider, leagueCode, matchday, homeTeam, awayTeam);
        if (baseMessage == null || baseMessage.isBlank()) {
            return suffix;
        }
        if (baseMessage.contains(CONTEXT_SEP + "слой:")
                || baseMessage.contains(CONTEXT_SEP + "данные о слое")) {
            return baseMessage;
        }
        return baseMessage + CONTEXT_SEP + suffix;
    }

    private static String buildContextSuffix(
            String layer,
            String provider,
            String leagueCode,
            Integer matchday,
            String homeTeam,
            String awayTeam
    ) {
        String layerPart = layer != null ? "слой: " + layer : "данные о слое недоступны";
        String providerPart = provider != null ? "провайдер: " + provider : "данные о провайдере недоступны";
        String leaguePart = leagueCode != null ? "лига: " + leagueCode : "данные о лиге недоступны";
        String matchdayPart = matchday != null ? "тур: " + matchday : "данные о туре недоступны";
        String teamsPart;
        if (homeTeam != null && awayTeam != null) {
            teamsPart = "команды: " + homeTeam + " — " + awayTeam;
        } else if (homeTeam != null || awayTeam != null) {
            teamsPart = "команды: "
                    + (homeTeam != null ? homeTeam : "?")
                    + " — "
                    + (awayTeam != null ? awayTeam : "?");
        } else {
            teamsPart = "информация о командах недоступна";
        }
        return layerPart + "; " + providerPart + "; " + leaguePart + "; " + matchdayPart + "; " + teamsPart;
    }

    private static final class ResolvedEntry {
        final String layer;
        final String provider;
        final String leagueCode;
        final String season;
        final Integer matchday;
        final String matchScheduleId;
        final String homeTeam;
        final String awayTeam;
        final String message;

        ResolvedEntry(
                String layer,
                String provider,
                String leagueCode,
                String season,
                Integer matchday,
                String matchScheduleId,
                String homeTeam,
                String awayTeam,
                String message
        ) {
            this.layer = layer;
            this.provider = provider;
            this.leagueCode = leagueCode;
            this.season = season;
            this.matchday = matchday;
            this.matchScheduleId = matchScheduleId;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.message = message;
        }
    }

    /**
     * Logs failed outbound HTTP from a monitoring run when callers did not already record
     * {@link #CODE_PROVIDER_FETCH_FAILED} with the same provider, layer, league and message.
     */
    public void recordHttpRequestFailuresIfNeeded(
            ExternalDataLayer layer,
            String provider,
            String leagueCode,
            String season,
            List<ExternalApiHttpLogEntry> httpLogs,
            String errorSummary
    ) {
        recordHttpRequestFailuresIfNeeded(layer, provider, leagueCode, season, httpLogs, errorSummary, List.of());
    }

    public void recordHttpRequestFailuresIfNeeded(
            ExternalDataLayer layer,
            String provider,
            String leagueCode,
            String season,
            List<ExternalApiHttpLogEntry> httpLogs,
            String errorSummary,
            List<String> failedMatchScheduleIds
    ) {
        int failed = countFailedHttpLogs(httpLogs);
        if (failed <= 0) {
            return;
        }
        String message = buildHttpFailureMessage(httpLogs, errorSummary);
        if (message == null || message.isBlank()) {
            return;
        }
        String providerNorm = blankToNull(provider);
        String layerName = layer != null ? layer.name() : null;
        String leagueNorm = blankToNull(leagueCode);
        Map<String, String> context = new LinkedHashMap<>();
        putFailedMatchIds(context, failedMatchScheduleIds);
        Entry entry = Entry.builder()
                .severity(SEVERITY_ERROR)
                .layer(layerName)
                .provider(provider)
                .code(CODE_PROVIDER_FETCH_FAILED)
                .message(message.trim())
                .leagueCode(leagueCode)
                .season(blankToNull(season))
                .context(context)
                .build();
        if (providerNorm != null
                && errorLogRepository.findFirstByProviderAndCodeAndLayerAndLeagueCodeAndMessage(
                providerNorm,
                CODE_PROVIDER_FETCH_FAILED,
                layerName,
                leagueNorm,
                toStoredMessage(entry)).isPresent()) {
            return;
        }
        record(entry);
    }

    /**
     * Persists a provider-level error_log from an already-built summary (e.g. ODDS mappingFailures with team labels)
     * when there were no failed HTTP rows to trigger {@link #recordHttpRequestFailuresIfNeeded}.
     */
    public void recordProviderMessageIfNeeded(
            ExternalDataLayer layer,
            String provider,
            String leagueCode,
            String season,
            String message
    ) {
        recordProviderMessageIfNeeded(layer, provider, leagueCode, season, message, List.of());
    }

    public void recordProviderMessageIfNeeded(
            ExternalDataLayer layer,
            String provider,
            String leagueCode,
            String season,
            String message,
            List<String> failedMatchScheduleIds
    ) {
        if (message == null || message.isBlank()) {
            return;
        }
        String providerNorm = blankToNull(provider);
        String layerName = layer != null ? layer.name() : null;
        String leagueNorm = blankToNull(leagueCode);
        String trimmed = message.trim();
        Map<String, String> context = new LinkedHashMap<>();
        putFailedMatchIds(context, failedMatchScheduleIds);
        Entry entry = Entry.builder()
                .severity(SEVERITY_ERROR)
                .layer(layerName)
                .provider(provider)
                .code(CODE_PROVIDER_FETCH_FAILED)
                .message(trimmed)
                .leagueCode(leagueCode)
                .season(blankToNull(season))
                .context(context)
                .build();
        if (providerNorm != null
                && errorLogRepository.findFirstByProviderAndCodeAndLayerAndLeagueCodeAndMessage(
                providerNorm,
                CODE_PROVIDER_FETCH_FAILED,
                layerName,
                leagueNorm,
                toStoredMessage(entry)).isPresent()) {
            return;
        }
        record(entry);
    }

    private static void putFailedMatchIds(Map<String, String> context, List<String> failedMatchScheduleIds) {
        if (context == null || failedMatchScheduleIds == null || failedMatchScheduleIds.isEmpty()) {
            return;
        }
        String joined = failedMatchScheduleIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        if (!joined.isEmpty()) {
            context.put("failedMatchScheduleIds", joined);
        }
    }

    private static String buildHttpFailureMessage(List<ExternalApiHttpLogEntry> httpLogs, String errorSummary) {
        StringBuilder sb = new StringBuilder();
        if (errorSummary != null && !errorSummary.isBlank()) {
            sb.append(errorSummary.trim());
        }
        int total = httpLogs != null ? httpLogs.size() : 0;
        int failed = countFailedHttpLogs(httpLogs);
        int success = Math.max(0, total - failed);
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append("httpSuccess=").append(success).append("/").append(total);
        if (httpLogs != null) {
            for (ExternalApiHttpLogEntry entry : httpLogs) {
                if (entry.getOutcome() == null || "SUCCESS".equals(entry.getOutcome())) {
                    continue;
                }
                sb.append("; ");
                sb.append(entry.getRequestType() != null ? entry.getRequestType() : "?");
                sb.append(":").append(entry.getOutcome());
                if (entry.getTeams() != null && !entry.getTeams().isBlank()) {
                    sb.append(" (").append(entry.getTeams().trim()).append(")");
                }
                if (entry.getDetail() != null && !entry.getDetail().isBlank()) {
                    sb.append(" — ").append(entry.getDetail().trim());
                }
            }
        }
        return sb.toString();
    }

    private static int countFailedHttpLogs(List<ExternalApiHttpLogEntry> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.stream()
                .filter(e -> e.getOutcome() != null && !"SUCCESS".equals(e.getOutcome()))
                .count();
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

    /**
     * One summary row when team-alias sync detected stored aliases that differ from the API names.
     */
    public void recordTeamAliasMismatchSummary(
            String provider,
            String leagueCode,
            List<TeamAliasMismatchDetail> mismatches,
            boolean overwritten
    ) {
        if (mismatches == null || mismatches.isEmpty()) {
            return;
        }
        Map<String, String> context = new LinkedHashMap<>();
        context.put("count", String.valueOf(mismatches.size()));
        context.put("overwritten", String.valueOf(overwritten));
        StringBuilder details = new StringBuilder();
        for (TeamAliasMismatchDetail mismatch : mismatches) {
            if (!details.isEmpty()) {
                details.append("; ");
            }
            details.append(mismatch.getTeamTitle())
                    .append(": «")
                    .append(mismatch.getCurrentAlias())
                    .append("» → «")
                    .append(mismatch.getIncomingAlias())
                    .append('»');
        }
        context.put("details", details.toString());

        String action = overwritten
                ? "алиасы перезаписаны при принудительной синхронизации"
                : "алиасы оставлены без изменений";
        // Include per-team current→incoming aliases in message so /error-logs shows them without digging into context.
        String message = "Рассинхрон алиаса у "
                + mismatches.size() + " " + teamsCountLabel(mismatches.size()) + ": " + action
                + ". " + details;

        record(Entry.builder()
                .severity(SEVERITY_WARN)
                .provider(provider)
                .code(CODE_TEAM_ALIAS_MISMATCH)
                .message(message)
                .leagueCode(leagueCode)
                .context(context)
                .build());
    }

    @Value
    @Builder
    public static class TeamAliasMismatchDetail {
        String teamId;
        String teamTitle;
        String currentAlias;
        String incomingAlias;
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
        String code = blankToNull(message) != null ? message.trim() : CODE_FULL_MATCH_FAILED;
        record(Entry.builder()
                .severity(SEVERITY_ERROR)
                .layer(ExternalDataLayer.FULL_MATCH.name())
                .provider(provider)
                .code(code)
                .message(message)
                .leagueCode(match != null ? match.getLeagueCode() : null)
                .matchday(match != null ? match.getMatchday() : null)
                .matchScheduleId(match != null ? match.getId() : null)
                .externalMatchId(null)
                .dedupeByMatch(true)
                .build());
    }

    public void recordFullMatchNotReady(MatchSchedule match, String provider, String providerStatus) {
        record(Entry.builder()
                .severity(SEVERITY_WARN)
                .layer(ExternalDataLayer.FULL_MATCH.name())
                .provider(provider)
                .code(CODE_FULL_MATCH_NOT_READY)
                .message(providerStatus != null && !providerStatus.isBlank()
                        ? providerStatus.trim()
                        : CODE_FULL_MATCH_NOT_READY)
                .leagueCode(match != null ? match.getLeagueCode() : null)
                .season(match != null ? match.getSeasonId() : null)
                .matchday(match != null ? match.getMatchday() : null)
                .matchScheduleId(match != null ? match.getId() : null)
                .dedupeByMatch(true)
                .build());
    }

    public static final int DEFAULT_PAGE_SIZE = 20;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100, 500);

    @Transactional(readOnly = true)
    public List<ErrorLogDto> listRecent(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("errorLogsInvalidPage");
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("errorLogsInvalidPageSize");
        }
        int skip = page * size;
        List<ErrorLogDto> dtos = ErrorLogDto.fromList(errorLogRepository.findRecent(skip, size));
        enrichFromMatchSchedules(dtos);
        enrichMessages(dtos);
        return dtos;
    }

    /** Ensure message always carries layer/provider/league/matchday/teams (also for legacy rows). */
    private static void enrichMessages(List<ErrorLogDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (ErrorLogDto dto : dtos) {
            String home = blankToNull(dto.getHomeTeamTitle());
            if (home == null) {
                home = blankToNull(dto.getHomeTeam());
            }
            String away = blankToNull(dto.getAwayTeamTitle());
            if (away == null) {
                away = blankToNull(dto.getAwayTeam());
            }
            dto.setMessage(formatInformativeMessage(
                    dto.getMessage(),
                    blankToNull(dto.getLayer()),
                    blankToNull(dto.getProvider()),
                    blankToNull(dto.getLeagueCode()),
                    dto.getMatchday(),
                    home,
                    away
            ));
        }
    }

    /**
     * Resolve home/away titles and logo keys from {@code match_schedules} when the log has a match id
     * or {@code context.failedMatchScheduleIds}.
     * Stored {@code homeTeam}/{@code awayTeam} names are kept if already present.
     */
    private void enrichFromMatchSchedules(List<ErrorLogDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        Set<String> scheduleIds = new LinkedHashSet<>();
        for (ErrorLogDto dto : dtos) {
            String id = dto.getMatchScheduleId();
            if (id != null && !id.isBlank()) {
                scheduleIds.add(id.trim());
            }
            scheduleIds.addAll(parseFailedMatchIds(dto.getContext()));
        }
        if (scheduleIds.isEmpty()) {
            return;
        }
        Map<String, MatchSchedule> schedules = new HashMap<>();
        for (MatchSchedule schedule : matchScheduleRepository.findAllById(scheduleIds)) {
            if (schedule.getId() != null) {
                schedules.put(schedule.getId(), schedule);
            }
        }
        if (schedules.isEmpty()) {
            return;
        }
        Set<String> teamIds = new HashSet<>();
        for (MatchSchedule schedule : schedules.values()) {
            if (schedule.getHomeTeamId() != null && !schedule.getHomeTeamId().isBlank()) {
                teamIds.add(schedule.getHomeTeamId());
            }
            if (schedule.getAwayTeamId() != null && !schedule.getAwayTeamId().isBlank()) {
                teamIds.add(schedule.getAwayTeamId());
            }
        }
        Map<String, Team> teams = new HashMap<>();
        if (!teamIds.isEmpty()) {
            for (Team team : teamsRepository.findAllById(teamIds)) {
                if (team.getId() != null) {
                    teams.put(team.getId(), team);
                }
            }
        }
        for (ErrorLogDto dto : dtos) {
            if (dto.getMatchScheduleId() != null && !dto.getMatchScheduleId().isBlank()) {
                MatchSchedule schedule = schedules.get(dto.getMatchScheduleId().trim());
                if (schedule != null) {
                    applyTeamFromSchedule(dto, true, teams.get(schedule.getHomeTeamId()));
                    applyTeamFromSchedule(dto, false, teams.get(schedule.getAwayTeamId()));
                }
            }
            List<String> failedIds = parseFailedMatchIds(dto.getContext());
            if (!failedIds.isEmpty()) {
                List<ExternalApiMatchTeamsDto> failedMatches = new ArrayList<>();
                for (String failedId : failedIds) {
                    MatchSchedule schedule = schedules.get(failedId);
                    if (schedule == null) {
                        continue;
                    }
                    Team home = teams.get(schedule.getHomeTeamId());
                    Team away = teams.get(schedule.getAwayTeamId());
                    failedMatches.add(ExternalApiMatchTeamsDto.builder()
                            .matchScheduleId(failedId)
                            .homeTitle(teamTitleOrId(home, schedule.getHomeTeamId()))
                            .awayTitle(teamTitleOrId(away, schedule.getAwayTeamId()))
                            .homeLogoKey(logoKeyOrTitle(home))
                            .awayLogoKey(logoKeyOrTitle(away))
                            .build());
                }
                if (!failedMatches.isEmpty()) {
                    dto.setFailedMatches(failedMatches);
                }
            }
        }
    }

    private static List<String> parseFailedMatchIds(Map<String, String> context) {
        if (context == null) {
            return List.of();
        }
        String raw = context.get("failedMatchScheduleIds");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    private static String teamTitleOrId(Team team, String fallbackId) {
        if (team != null && team.getTitle() != null && !team.getTitle().isBlank()) {
            return team.getTitle().trim();
        }
        return fallbackId;
    }

    private static String logoKeyOrTitle(Team team) {
        if (team == null) {
            return null;
        }
        if (team.getLogo() != null && !team.getLogo().isBlank()) {
            return team.getLogo().trim();
        }
        if (team.getTitle() != null && !team.getTitle().isBlank()) {
            return team.getTitle().trim();
        }
        return null;
    }

    private static void applyTeamFromSchedule(ErrorLogDto dto, boolean home, Team team) {
        if (team == null) {
            return;
        }
        String logo = logoKeyOrTitle(team);
        if (home) {
            dto.setHomeTeamTitle(team.getTitle());
            dto.setHomeTeamLogoKey(logo);
            if (dto.getHomeTeam() == null || dto.getHomeTeam().isBlank()) {
                dto.setHomeTeam(team.getTitle());
            }
        } else {
            dto.setAwayTeamTitle(team.getTitle());
            dto.setAwayTeamLogoKey(logo);
            if (dto.getAwayTeam() == null || dto.getAwayTeam().isBlank()) {
                dto.setAwayTeam(team.getTitle());
            }
        }
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

    /**
     * Appends {@code now} to the incident timeline. {@code createdAt} stays the first failure.
     * If an older row only has {@code occurrenceCount} (no timestamps), the count is preserved
     * and new Instants start accumulating from this moment.
     */
    private static void appendOccurrence(ErrorLog doc, Instant now) {
        List<Instant> times = doc.getOccurredAt() != null ? new ArrayList<>(doc.getOccurredAt()) : new ArrayList<>();
        if (times.isEmpty()) {
            Instant first = doc.getFirstOccurredAt() != null ? doc.getFirstOccurredAt() : doc.getCreatedAt();
            if (first != null) {
                times.add(first);
            }
        }
        times.add(now);
        doc.setOccurredAt(times);
        int fromArray = times.size();
        int prevCount = doc.getOccurrenceCount() == null || doc.getOccurrenceCount() < 1
                ? 0
                : doc.getOccurrenceCount();
        doc.setOccurrenceCount(Math.max(fromArray, prevCount + 1));
        doc.setLastOccurredAt(now);
        Instant first = times.get(0);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(first);
        }
        if (doc.getFirstOccurredAt() == null) {
            doc.setFirstOccurredAt(first);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String teamsCountLabel(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod100 >= 11 && mod100 <= 14) {
            return "команд";
        }
        return mod10 == 1 ? "команды" : "команд";
    }
}
