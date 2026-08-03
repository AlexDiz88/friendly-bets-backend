package net.friendly_bets.providers.live;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.twentyfourscore.LiveMinuteLabelResolver;

import java.time.Instant;

/**
 * Shared write path for LIVE updates into {@code match_schedules}.
 * Minute labels are normalized to UI style ({@code 45+}, {@code 90+}, {@code 105+}, {@code 120+}).
 */
public final class LiveMatchApplySupport {

    private LiveMatchApplySupport() {
    }

    public static void apply(MatchSchedule schedule, LiveMatchSnapshot snapshot, Instant now) {
        if (schedule == null || snapshot == null) {
            return;
        }
        schedule.setStatus(snapshot.status());
        if (LiveMatchSupport.isFinishedStatus(snapshot.status())
                && schedule.getLiveFinishedDetectedAt() == null) {
            schedule.setLiveFinishedDetectedAt(now != null ? now : Instant.now());
        }
        if (snapshot.rawMinuteLabel() != null) {
            String resolvedLabel = LiveMinuteLabelResolver.resolve(
                    snapshot.rawMinuteLabel(),
                    schedule.getUtcKickoff(),
                    now != null ? now : Instant.now(),
                    schedule.getLeagueCode(),
                    schedule.getSlotId(),
                    snapshot.status()
            );
            schedule.setLiveMinuteLabel(resolvedLabel);
            Integer minute = LiveMinuteLabelResolver.parseMinuteInteger(snapshot.rawMinuteLabel());
            if (minute != null) {
                schedule.setLiveMinute(minute);
            }
        } else if (LiveMatchSupport.isFinishedStatus(snapshot.status())
                || LiveMatchSupport.isCanceledStatus(snapshot.status())) {
            schedule.setLiveMinute(null);
            schedule.setLiveMinuteLabel(null);
        }
        if (snapshot.fullTimeScore() != null || snapshot.penaltyScore() != null) {
            GameScore score = schedule.getGameScore() != null ? schedule.getGameScore() : new GameScore();
            if (snapshot.fullTimeScore() != null) {
                score.setFullTime(snapshot.fullTimeScore());
            }
            if (snapshot.firstTimeScore() != null) {
                score.setFirstTime(snapshot.firstTimeScore());
            }
            if (snapshot.penaltyScore() != null) {
                score.setPenalty(snapshot.penaltyScore());
            }
            schedule.setGameScore(score);
        }
        schedule.setFetchedAt(Instant.now());
    }
}
