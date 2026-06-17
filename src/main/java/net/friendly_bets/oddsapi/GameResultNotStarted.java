package net.friendly_bets.oddsapi;

import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.wc26.Wc26ScheduleKickoffLookup;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

public final class GameResultNotStarted {

    private static final Set<String> NOT_STARTED_STATUSES = Set.of("SCHEDULED", "TIMED");

    private GameResultNotStarted() {
    }

    /**
     * Текущее время в UTC. GameResultRecord.utcDate хранится в UTC,
     * поэтому все сравнения «матч начался?» обязаны использовать эти часы,
     * а не LocalDateTime.now() с зоной JVM.
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static boolean isNotStarted(GameResultRecord match) {
        return isNotStarted(match, nowUtc());
    }

    public static boolean isNotStarted(GameResultRecord match, LocalDateTime now) {
        if (match == null) {
            return false;
        }
        String status = normalizeStatus(match.getStatus());
        if (status != null && !NOT_STARTED_STATUSES.contains(status)) {
            return false;
        }
        LocalDateTime kickoff = resolveKickoffUtc(match);
        if (kickoff != null && !kickoff.isAfter(now)) {
            return false;
        }
        return true;
    }

    /**
     * Kickoff UTC для сравнения «матч начался?». Для ЧМ26 с wc26_schedule_id — из каталога расписания
     * (см. {@link GameResultEffectiveKickoff}); статический fallback без Spring-контекста.
     */
    public static LocalDateTime resolveKickoffUtc(GameResultRecord match) {
        if (match == null) {
            return null;
        }
        if (match.getWc26ScheduleId() != null) {
            return Wc26ScheduleKickoffLookup.kickoffUtc(match.getWc26ScheduleId())
                    .orElse(match.getUtcDate());
        }
        return match.getUtcDate();
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "PAUSE", "HALFTIME" -> "PAUSED";
            default -> status;
        };
    }
}
