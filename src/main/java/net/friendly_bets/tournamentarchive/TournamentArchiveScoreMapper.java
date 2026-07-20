package net.friendly_bets.tournamentarchive;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.fifa.FifaMatchParser;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveMatch;

/**
 * Best-effort маппинг счёта FIFA → {@link GameScore}.
 * HT / раздельный FT при AET обычно недоступны — оставляем null, правится вручную в JSON.
 */
public final class TournamentArchiveScoreMapper {

    private TournamentArchiveScoreMapper() {
    }

    public static void applyScore(TournamentArchiveMatch match, JsonNode fifaMatch) {
        Integer homeScore = FifaMatchParser.homeScore(fifaMatch);
        Integer awayScore = FifaMatchParser.awayScore(fifaMatch);
        Integer homePen = intOrNull(fifaMatch.get("HomeTeamPenaltyScore"));
        Integer awayPen = intOrNull(fifaMatch.get("AwayTeamPenaltyScore"));
        Integer resultType = intOrNull(fifaMatch.get("ResultType"));

        if (!FifaMatchParser.isFinished(fifaMatch) || homeScore == null || awayScore == null) {
            match.setGameScore(null);
            return;
        }

        boolean hasPens = homePen != null && awayPen != null && !homePen.equals(awayPen);
        boolean likelyExtraTime = resultType != null && (resultType == 2 || resultType == 3);

        GameScore.GameScoreBuilder scoreBuilder = GameScore.builder()
                .fullTime(null)
                .firstTime(null)
                .overTime(null)
                .penalty(null);

        if (hasPens) {
            scoreBuilder.overTime(score(homeScore, awayScore));
            scoreBuilder.penalty(score(homePen, awayPen));
        } else if (likelyExtraTime || isKnockout(match.getStage()) && resultType != null && resultType == 2) {
            scoreBuilder.overTime(score(homeScore, awayScore));
        } else {
            scoreBuilder.fullTime(score(homeScore, awayScore));
        }

        match.setGameScore(scoreBuilder.build());
    }

    private static boolean isKnockout(String stage) {
        return TournamentArchiveStages.isKnockout(stage);
    }

    private static String score(int home, int away) {
        return home + ":" + away;
    }

    private static Integer intOrNull(JsonNode node) {
        return node != null && node.isNumber() ? node.asInt() : null;
    }
}
