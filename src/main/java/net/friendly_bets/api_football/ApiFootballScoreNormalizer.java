package net.friendly_bets.api_football;

import net.friendly_bets.api_football.client.dto.ApiFootballFixtureDto;
import net.friendly_bets.footballdata.FootballDataScoreNormalizer;
import net.friendly_bets.models.GameScore;
import org.springframework.stereotype.Component;

@Component
public class ApiFootballScoreNormalizer {

    public GameScore normalize(ApiFootballFixtureDto dto) {
        if (dto == null || dto.getScore() == null || dto.getGoals() == null) {
            return null;
        }
        ApiFootballFixtureDto.Score score = dto.getScore();
        if (score.getFulltime() == null
                || score.getFulltime().getHome() == null
                || score.getFulltime().getAway() == null) {
            return null;
        }

        GameScore.GameScoreBuilder builder = GameScore.builder()
                .fullTime(format(score.getFulltime()));

        if (score.getHalftime() != null
                && score.getHalftime().getHome() != null
                && score.getHalftime().getAway() != null) {
            builder.firstTime(format(score.getHalftime()));
        }

        boolean hasExtra = score.getExtratime() != null
                && score.getExtratime().getHome() != null
                && score.getExtratime().getAway() != null;
        boolean hasPenalty = score.getPenalty() != null
                && score.getPenalty().getHome() != null
                && score.getPenalty().getAway() != null;

        if (hasExtra) {
            builder.overTime(format(score.getExtratime()));
        }
        if (hasPenalty) {
            builder.penalty(format(score.getPenalty()));
        }

        return builder.build();
    }

    public String resolveDuration(ApiFootballFixtureDto dto) {
        if (dto == null || dto.getScore() == null) {
            return FootballDataScoreNormalizer.DURATION_REGULAR;
        }
        boolean hasPenalty = dto.getScore().getPenalty() != null
                && dto.getScore().getPenalty().getHome() != null
                && dto.getScore().getPenalty().getAway() != null;
        if (hasPenalty) {
            return FootballDataScoreNormalizer.DURATION_PENALTY_SHOOTOUT;
        }
        boolean hasExtra = dto.getScore().getExtratime() != null
                && dto.getScore().getExtratime().getHome() != null
                && dto.getScore().getExtratime().getAway() != null;
        if (hasExtra) {
            return FootballDataScoreNormalizer.DURATION_EXTRA_TIME;
        }
        return FootballDataScoreNormalizer.DURATION_REGULAR;
    }

    private static String format(ApiFootballFixtureDto.Part part) {
        return part.getHome() + ":" + part.getAway();
    }
}
