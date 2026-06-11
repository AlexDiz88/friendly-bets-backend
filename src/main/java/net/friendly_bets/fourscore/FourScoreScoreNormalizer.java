package net.friendly_bets.fourscore;

import net.friendly_bets.footballdata.FootballDataScoreNormalizer;
import net.friendly_bets.footballdata.FootballDataMatchStatuses;
import net.friendly_bets.models.GameScore;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FourScoreScoreNormalizer {

    public record NormalizedScore(GameScore gameScore, String scoreDuration, String status) {
    }

    public NormalizedScore normalize(FourScoreEventDetails details) {
        if (details == null) {
            return null;
        }
        String status = mapStatus(details.getStatusText());
        if (!FootballDataMatchStatuses.hasScore(status) && !FootballDataMatchStatuses.isTerminal(status)) {
            return new NormalizedScore(null, null, status);
        }

        String firstHalf = details.getFirstHalfScore();
        String secondHalf = details.getSecondHalfScore();
        String fullTime = resolveFullTime(firstHalf, secondHalf, details);
        if (fullTime == null) {
            return new NormalizedScore(null, null, status);
        }

        GameScore.GameScoreBuilder builder = GameScore.builder()
                .fullTime(fullTime)
                .firstTime(firstHalf);

        if (details.getExtraTimeScore() != null && !details.getExtraTimeScore().isBlank()) {
            builder.overTime(details.getExtraTimeScore());
        }
        if (details.getPenaltyScore() != null && !details.getPenaltyScore().isBlank()) {
            builder.penalty(details.getPenaltyScore());
        }

        String duration = resolveDuration(details);
        return new NormalizedScore(builder.build(), duration, status);
    }

    private static String resolveDuration(FourScoreEventDetails details) {
        if (details.getPenaltyScore() != null && !details.getPenaltyScore().isBlank()) {
            return FootballDataScoreNormalizer.DURATION_PENALTY_SHOOTOUT;
        }
        if (details.getExtraTimeScore() != null && !details.getExtraTimeScore().isBlank()) {
            return FootballDataScoreNormalizer.DURATION_EXTRA_TIME;
        }
        return FootballDataScoreNormalizer.DURATION_REGULAR;
    }

    private static String resolveFullTime(String firstHalf, String secondHalf, FourScoreEventDetails details) {
        if (firstHalf != null && secondHalf != null) {
            int[] fh = parseParts(firstHalf);
            int[] sh = parseParts(secondHalf);
            if (fh != null && sh != null) {
                return (fh[0] + sh[0]) + ":" + (fh[1] + sh[1]);
            }
        }
        if (details.getHeaderHomeScore() != null && details.getHeaderAwayScore() != null
                && details.getExtraTimeScore() == null && details.getPenaltyScore() == null) {
            return details.getHeaderHomeScore() + ":" + details.getHeaderAwayScore();
        }
        return null;
    }

    private static int[] parseParts(String score) {
        if (score == null || !score.contains(":")) {
            return null;
        }
        String[] parts = score.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String mapStatus(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return "SCHEDULED";
        }
        String lower = statusText.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("завершено")) {
            return "FINISHED";
        }
        if (lower.contains("отмен")) {
            return "CANCELLED";
        }
        if (lower.contains("отлож")) {
            return "POSTPONED";
        }
        if (lower.contains("перерыв")) {
            return "PAUSED";
        }
        if (lower.contains("идёт") || lower.contains("идет")) {
            return "IN_PLAY";
        }
        if (lower.contains("не началось")) {
            return "SCHEDULED";
        }
        return "SCHEDULED";
    }
}
