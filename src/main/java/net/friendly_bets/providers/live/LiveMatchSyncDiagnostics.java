package net.friendly_bets.providers.live;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.ErrorLogService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Detects LIVE matches that stay unresolved too long or disappear from provider feeds.
 */
@Component
@RequiredArgsConstructor
public class LiveMatchSyncDiagnostics {

    /** Typical FT + short stoppage — first operator-visible warning. */
    public static final long WARN_AFTER_KICKOFF_SECONDS = Duration.ofMinutes(165).toSeconds();
    /** Max regulation + ET + pens buffer — error threshold. */
    public static final long ERROR_AFTER_KICKOFF_SECONDS = Duration.ofMinutes(225).toSeconds();
    /** Not found in feed after kickoff — start warning. */
    public static final long NOT_IN_FEED_WARN_AFTER_KICKOFF_SECONDS = Duration.ofHours(1).toSeconds();
    /** Kickoff passed but LIVE never wrote fetched_at — scheduler/poll gap. */
    public static final long NEVER_POLLED_WARN_AFTER_KICKOFF_SECONDS = Duration.ofMinutes(20).toSeconds();

    private final ErrorLogService errorLogService;
    private final TeamsRepository teamsRepository;

    /**
     * Layer-level: kickoff already passed, status still non-terminal, LIVE never updated the row.
     * Called from the wake scheduler even when {@code syncLive} did not run.
     */
    public void reportNeverPolledAfterKickoff(String seasonId, List<MatchSchedule> schedules, Instant now) {
        if (schedules == null || schedules.isEmpty() || now == null) {
            return;
        }
        Map<String, Team> teamCache = new HashMap<>();
        for (MatchSchedule schedule : schedules) {
            if (schedule == null || schedule.getId() == null || schedule.getId().isBlank()) {
                continue;
            }
            Instant kickoff = schedule.getUtcKickoff();
            if (kickoff == null || kickoff.isAfter(now)) {
                continue;
            }
            if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
                continue;
            }
            String status = schedule.getStatus();
            if (LiveMatchSupport.isTerminalNoPoll(status)) {
                continue;
            }
            Instant fetchedAt = schedule.getFetchedAt();
            boolean neverPolledAfterKickoff = fetchedAt == null || fetchedAt.isBefore(kickoff);
            if (!neverPolledAfterKickoff) {
                continue;
            }
            long sinceKickoffSec = Math.max(0, Duration.between(kickoff, now).getSeconds());
            if (sinceKickoffSec < NEVER_POLLED_WARN_AFTER_KICKOFF_SECONDS) {
                continue;
            }
            String severity = sinceKickoffSec >= ERROR_AFTER_KICKOFF_SECONDS
                    ? ErrorLogService.SEVERITY_ERROR
                    : ErrorLogService.SEVERITY_WARN;
            Map<String, String> context = baseContext(schedule, teamCache);
            context.put("sinceKickoffSec", String.valueOf(sinceKickoffSec));
            context.put("status", status != null ? status : "");
            context.put("fetchedAt", fetchedAt != null ? fetchedAt.toString() : "");
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(severity)
                    .layer(ExternalDataLayer.LIVE.name())
                    .provider(null)
                    .code(ErrorLogService.CODE_LIVE_MATCH_NEVER_POLLED)
                    .message("LIVE не опрашивал матч после kickoff (" + formatDuration(sinceKickoffSec)
                            + "), статус " + (status != null ? status : "?")
                            + " — wake/sync не обновил match_schedules")
                    .leagueCode(schedule.getLeagueCode())
                    .season(seasonId)
                    .matchday(schedule.getMatchday())
                    .matchScheduleId(schedule.getId())
                    .homeTeam(teamName(schedule.getHomeTeamId(), teamCache))
                    .awayTeam(teamName(schedule.getAwayTeamId(), teamCache))
                    .context(context)
                    .dedupeByMatch(true)
                    .build());
        }
    }

    public void afterSync(
            String providerId,
            String seasonId,
            List<MatchSchedule> tracked,
            Set<String> updatedScheduleIds,
            Set<String> notFoundScheduleIds,
            Instant now
    ) {
        if (providerId == null || providerId.isBlank() || tracked == null || tracked.isEmpty() || now == null) {
            return;
        }
        Map<String, Team> teamCache = new HashMap<>();
        for (MatchSchedule schedule : tracked) {
            if (schedule == null || schedule.getId() == null) {
                continue;
            }
            String id = schedule.getId();
            boolean updated = updatedScheduleIds != null && updatedScheduleIds.contains(id);
            boolean notFound = notFoundScheduleIds != null && notFoundScheduleIds.contains(id);
            Instant kickoff = schedule.getUtcKickoff();
            if (kickoff == null) {
                continue;
            }
            long sinceKickoffSec = Math.max(0, Duration.between(kickoff, now).getSeconds());
            String status = schedule.getStatus();
            boolean inPlayLike = isInPlayLike(status);

            if (notFound && sinceKickoffSec >= NOT_IN_FEED_WARN_AFTER_KICKOFF_SECONDS) {
                recordNotInFeed(providerId, seasonId, schedule, teamCache, sinceKickoffSec, status, updated);
            }

            if (!inPlayLike) {
                continue;
            }

            Instant fetchedAt = schedule.getFetchedAt();
            long sinceFetchedSec = fetchedAt != null
                    ? Math.max(0, Duration.between(fetchedAt, now).getSeconds())
                    : sinceKickoffSec;

            if (sinceKickoffSec >= ERROR_AFTER_KICKOFF_SECONDS) {
                recordStuck(
                        ErrorLogService.SEVERITY_ERROR,
                        ErrorLogService.CODE_LIVE_MATCH_SYNC_STUCK,
                        providerId,
                        seasonId,
                        schedule,
                        teamCache,
                        sinceKickoffSec,
                        sinceFetchedSec,
                        status,
                        updated,
                        notFound,
                        "kickoffAgeSec=" + sinceKickoffSec + " still " + status
                );
            } else if (sinceKickoffSec >= WARN_AFTER_KICKOFF_SECONDS
                    && sinceFetchedSec >= Duration.ofMinutes(30).toSeconds()) {
                recordStuck(
                        ErrorLogService.SEVERITY_WARN,
                        ErrorLogService.CODE_LIVE_MATCH_SYNC_STUCK,
                        providerId,
                        seasonId,
                        schedule,
                        teamCache,
                        sinceKickoffSec,
                        sinceFetchedSec,
                        status,
                        updated,
                        notFound,
                        "no terminal status for " + formatDuration(sinceKickoffSec)
                );
            }
        }
    }

    public void recordAmbiguous(
            String providerId,
            String seasonId,
            MatchSchedule schedule,
            int candidateCount
    ) {
        if (schedule == null || schedule.getId() == null || candidateCount <= 1) {
            return;
        }
        Map<String, String> context = baseContext(schedule, Map.of());
        context.put("candidateCount", String.valueOf(candidateCount));
        errorLogService.record(ErrorLogService.Entry.builder()
                .severity(ErrorLogService.SEVERITY_WARN)
                .layer(ExternalDataLayer.LIVE.name())
                .provider(providerId)
                .code(ErrorLogService.CODE_LIVE_MATCH_AMBIGUOUS)
                .message("Несколько строк провайдера для одного матча (" + candidateCount + " кандидатов)")
                .leagueCode(schedule.getLeagueCode())
                .season(seasonId)
                .matchScheduleId(schedule.getId())
                .homeTeam(resolveTeamName(schedule.getHomeTeamId()))
                .awayTeam(resolveTeamName(schedule.getAwayTeamId()))
                .context(context)
                .dedupeByMatch(true)
                .build());
    }

    private void recordNotInFeed(
            String providerId,
            String seasonId,
            MatchSchedule schedule,
            Map<String, Team> teamCache,
            long sinceKickoffSec,
            String status,
            boolean updatedThisRun
    ) {
        Map<String, String> context = baseContext(schedule, teamCache);
        context.put("sinceKickoffSec", String.valueOf(sinceKickoffSec));
        context.put("status", status != null ? status : "");
        context.put("updatedThisRun", String.valueOf(updatedThisRun));
        errorLogService.record(ErrorLogService.Entry.builder()
                .severity(ErrorLogService.SEVERITY_WARN)
                .layer(ExternalDataLayer.LIVE.name())
                .provider(providerId)
                .code(ErrorLogService.CODE_LIVE_MATCH_NOT_IN_FEED)
                .message("Матч не найден в ответе LIVE-провайдера спустя " + formatDuration(sinceKickoffSec)
                        + " после kickoff (статус " + (status != null ? status : "?") + ")")
                .leagueCode(schedule.getLeagueCode())
                .season(seasonId)
                .matchday(schedule.getMatchday())
                .matchScheduleId(schedule.getId())
                .homeTeam(teamName(schedule.getHomeTeamId(), teamCache))
                .awayTeam(teamName(schedule.getAwayTeamId(), teamCache))
                .context(context)
                .dedupeByMatch(true)
                .build());
    }

    private void recordStuck(
            String severity,
            String code,
            String providerId,
            String seasonId,
            MatchSchedule schedule,
            Map<String, Team> teamCache,
            long sinceKickoffSec,
            long sinceFetchedSec,
            String status,
            boolean updatedThisRun,
            boolean notFound,
            String reason
    ) {
        Map<String, String> context = baseContext(schedule, teamCache);
        context.put("sinceKickoffSec", String.valueOf(sinceKickoffSec));
        context.put("sinceFetchedSec", String.valueOf(sinceFetchedSec));
        context.put("status", status != null ? status : "");
        context.put("updatedThisRun", String.valueOf(updatedThisRun));
        context.put("notFoundInFeed", String.valueOf(notFound));
        context.put("reason", reason);
        errorLogService.record(ErrorLogService.Entry.builder()
                .severity(severity)
                .layer(ExternalDataLayer.LIVE.name())
                .provider(providerId)
                .code(code)
                .message("LIVE-синхронизация застряла: " + reason
                        + ", статус " + (status != null ? status : "?")
                        + ", kickoff+" + formatDuration(sinceKickoffSec)
                        + ", без обновления " + formatDuration(sinceFetchedSec))
                .leagueCode(schedule.getLeagueCode())
                .season(seasonId)
                .matchday(schedule.getMatchday())
                .matchScheduleId(schedule.getId())
                .homeTeam(teamName(schedule.getHomeTeamId(), teamCache))
                .awayTeam(teamName(schedule.getAwayTeamId(), teamCache))
                .context(context)
                .dedupeByMatch(true)
                .build());
    }

    private Map<String, String> baseContext(MatchSchedule schedule, Map<String, Team> teamCache) {
        Map<String, String> context = new LinkedHashMap<>();
        if (schedule.getUtcKickoff() != null) {
            context.put("utcKickoff", schedule.getUtcKickoff().toString());
        }
        if (schedule.getSlotId() != null) {
            context.put("slotId", schedule.getSlotId());
        }
        context.put("homeTeamId", schedule.getHomeTeamId());
        context.put("awayTeamId", schedule.getAwayTeamId());
        context.put("home", teamName(schedule.getHomeTeamId(), teamCache));
        context.put("away", teamName(schedule.getAwayTeamId(), teamCache));
        return context;
    }

    private String teamName(String teamId, Map<String, Team> cache) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        Team team = cache.computeIfAbsent(teamId, id -> teamsRepository.findById(id).orElse(null));
        return team != null ? team.getTitle() : teamId;
    }

    private String resolveTeamName(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return teamsRepository.findById(teamId).map(Team::getTitle).orElse(teamId);
    }

    private static boolean isInPlayLike(String status) {
        if (status == null) {
            return false;
        }
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "IN_PLAY".equals(n) || "LIVE".equals(n) || "PAUSED".equals(n) || "HALFTIME".equals(n)
                || "EXTRA_TIME".equals(n) || "PENALTY_SHOOTOUT".equals(n);
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h" + minutes + "m";
        }
        return minutes + "m";
    }
}
