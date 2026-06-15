package net.friendly_bets.twentyfourscore;

import net.friendly_bets.gameresults.CanonicalScoreNormalizer;
import net.friendly_bets.gameresults.MatchStatuses;
import net.friendly_bets.models.GameScore;
import org.springframework.stereotype.Component;

@Component
public class TwentyFourScoreScoreNormalizer {

    public record NormalizedScore(
            String status,
            GameScore gameScore,
            String scoreDuration,
            String liveMinuteLabel
    ) {
    }

    public NormalizedScore normalize(TwentyFourScoreMatchDetails details) {
        if (details == null) {
            return null;
        }
        return normalize(
                details.getStatusText(),
                details.getFullTimeScore(),
                details.getFirstHalfScore(),
                details.getExtraTimeScore(),
                details.getPenaltyScore(),
                details.getLiveMinuteLabel()
        );
    }

    public NormalizedScore normalize(TwentyFourScoreListMatch listMatch) {
        if (listMatch == null) {
            return null;
        }
        return normalize(
                listMatch.getStatusText(),
                listMatch.getFullTimeScore(),
                listMatch.getFirstHalfScore(),
                listMatch.getExtraTimeScore(),
                listMatch.getPenaltyScore(),
                listMatch.getLiveMinuteLabel()
        );
    }

    private NormalizedScore normalize(
            String statusText,
            String fullTime,
            String firstHalf,
            String extraTime,
            String penalty,
            String liveMinuteLabel
    ) {
        String status = TwentyFourScoreStatusMapper.mapStatus(statusText);
        String minute = liveMinuteLabel != null
                ? liveMinuteLabel
                : TwentyFourScoreStatusMapper.extractLiveMinute(statusText);
        GameScore.GameScoreBuilder scoreBuilder = GameScore.builder();
        if (fullTime != null && !fullTime.isBlank()) {
            scoreBuilder.fullTime(fullTime.trim());
        } else if (MatchStatuses.LIVE.contains(status) && minute != null && !minute.isBlank()) {
            scoreBuilder.fullTime("0:0");
        }
        if (firstHalf != null && !firstHalf.isBlank()) {
            scoreBuilder.firstTime(firstHalf.trim());
        }
        if (extraTime != null && !extraTime.isBlank()) {
            scoreBuilder.overTime(extraTime.trim());
        }
        if (penalty != null && !penalty.isBlank()) {
            scoreBuilder.penalty(penalty.trim());
        }
        GameScore gameScore = scoreBuilder.build();
        String duration = resolveDuration(extraTime, penalty);
        return new NormalizedScore(status, gameScore, duration, minute);
    }

    private static String resolveDuration(String extraTime, String penalty) {
        if (penalty != null && !penalty.isBlank()) {
            return CanonicalScoreNormalizer.DURATION_PENALTY_SHOOTOUT;
        }
        if (extraTime != null && !extraTime.isBlank()) {
            return CanonicalScoreNormalizer.DURATION_EXTRA_TIME;
        }
        return CanonicalScoreNormalizer.DURATION_REGULAR;
    }
}
