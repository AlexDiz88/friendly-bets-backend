package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class GameResultMapper {

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ISO_DATE_TIME;

    public GameResultRecord toNewRecord(
            FootballDataMatchDto dto,
            String leagueCode,
            String season,
            int storageMatchday,
            Team homeTeam,
            Team awayTeam,
            String leagueId,
            String externalCompetitionCode,
            LocalDateTime fetchedAt
    ) {
        GameResultSourceSnapshot source = toSourceSnapshot(
                dto, externalCompetitionCode, season, fetchedAt);
        return GameResultRecord.builder()
                .leagueCode(leagueCode)
                .matchday(storageMatchday)
                .season(season)
                .leagueId(leagueId)
                .homeTeamId(homeTeam.getId())
                .awayTeamId(awayTeam.getId())
                .status(dto.getStatus())
                .utcDate(parseUtc(dto.getUtcDate()))
                .gameScore(toGameScore(dto))
                .fetchedAt(fetchedAt)
                .provider(MatchDataProviders.FOOTBALL_DATA)
                .sources(Map.of(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), source))
                .build();
    }

    public GameResultSourceSnapshot toSourceSnapshot(
            FootballDataMatchDto dto,
            String externalCompetitionCode,
            String storageSeason,
            LocalDateTime fetchedAt
    ) {
        return GameResultSourceSnapshot.builder()
                .externalMatchId(dto.getId())
                .externalCompetitionCode(externalCompetitionCode)
                .externalMatchday(dto.getMatchday())
                .externalSeason(storageSeason)
                .status(dto.getStatus())
                .utcDate(parseUtc(dto.getUtcDate()))
                .gameScore(toGameScore(dto))
                .home(sideSnapshot(dto.getHomeTeam().getId(), dto.getHomeTeam().getName()))
                .away(sideSnapshot(dto.getAwayTeam().getId(), dto.getAwayTeam().getName()))
                .apiLastUpdated(parseUtc(dto.getLastUpdated()))
                .fetchedAt(fetchedAt)
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

    private static GameResultSideSnapshot sideSnapshot(int externalTeamId, String externalName) {
        return GameResultSideSnapshot.builder()
                .externalId(String.valueOf(externalTeamId))
                .externalName(externalName)
                .build();
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
