package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.external.ExternalMatch;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class FootballDataMatchMapper {

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ISO_DATE_TIME;

    public ExternalMatch toEntity(FootballDataMatchDto dto,
                                  String competitionCode,
                                  String season,
                                  Team homeTeam,
                                  Team awayTeam,
                                  String leagueId,
                                  LocalDateTime fetchedAt) {
        return ExternalMatch.builder()
                .externalMatchId(dto.getId())
                .competitionCode(competitionCode)
                .matchday(dto.getMatchday())
                .season(season)
                .status(dto.getStatus())
                .utcDate(parseUtc(dto.getUtcDate()))
                .homeFootballDataTeamId(dto.getHomeTeam().getId())
                .awayFootballDataTeamId(dto.getAwayTeam().getId())
                .homeTeamName(dto.getHomeTeam().getName())
                .awayTeamName(dto.getAwayTeam().getName())
                .homeTeamId(homeTeam != null ? homeTeam.getId() : null)
                .awayTeamId(awayTeam != null ? awayTeam.getId() : null)
                .leagueId(leagueId)
                .gameScore(toGameScore(dto))
                .fetchedAt(fetchedAt)
                .apiLastUpdated(parseUtc(dto.getLastUpdated()))
                .build();
    }

    public GameScore toGameScore(FootballDataMatchDto dto) {
        if (dto.getScore() == null || !FootballDataMatchStatuses.hasScore(dto.getStatus())) {
            return null;
        }

        FootballDataMatchDto.Score score = dto.getScore();
        GameScore.GameScoreBuilder builder = GameScore.builder();

        if (score.getFullTime() != null && score.getFullTime().getHome() != null && score.getFullTime().getAway() != null) {
            builder.fullTime(formatScore(score.getFullTime().getHome(), score.getFullTime().getAway()));
        }
        if (score.getHalfTime() != null && score.getHalfTime().getHome() != null && score.getHalfTime().getAway() != null) {
            builder.firstTime(formatScore(score.getHalfTime().getHome(), score.getHalfTime().getAway()));
        }
        if (score.getExtraTime() != null && score.getExtraTime().getHome() != null && score.getExtraTime().getAway() != null) {
            builder.overTime(formatScore(score.getExtraTime().getHome(), score.getExtraTime().getAway()));
        }
        if (score.getPenalties() != null && score.getPenalties().getHome() != null && score.getPenalties().getAway() != null) {
            builder.penalty(formatScore(score.getPenalties().getHome(), score.getPenalties().getAway()));
        }

        GameScore gameScore = builder.build();
        if (gameScore.getFullTime() == null) {
            return null;
        }
        return gameScore;
    }

    private static String formatScore(int home, int away) {
        return home + ":" + away;
    }

    private static LocalDateTime parseUtc(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.endsWith("Z") ? value.substring(0, value.length() - 1) : value;
        return LocalDateTime.parse(normalized, API_DATE);
    }
}
