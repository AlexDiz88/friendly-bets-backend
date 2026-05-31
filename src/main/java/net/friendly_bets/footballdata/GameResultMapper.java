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

    private final FootballDataScoreNormalizer scoreNormalizer;

    public GameResultMapper(FootballDataScoreNormalizer scoreNormalizer) {
        this.scoreNormalizer = scoreNormalizer;
    }

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
                .gameScore(toCanonicalGameScore(dto))
                .scoreDuration(resolveScoreDuration(dto))
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
                .gameScore(scoreNormalizer.toRawApiScore(dto))
                .scoreDuration(resolveScoreDuration(dto))
                .home(sideSnapshot(dto.getHomeTeam().getId(), dto.getHomeTeam().getName()))
                .away(sideSnapshot(dto.getAwayTeam().getId(), dto.getAwayTeam().getName()))
                .apiLastUpdated(parseUtc(dto.getLastUpdated()))
                .fetchedAt(fetchedAt)
                .build();
    }

    public GameScore toCanonicalGameScore(FootballDataMatchDto dto) {
        return scoreNormalizer.normalize(dto);
    }

    /** @deprecated используйте {@link #toCanonicalGameScore(FootballDataMatchDto)} */
    @Deprecated
    public GameScore toGameScore(FootballDataMatchDto dto) {
        return toCanonicalGameScore(dto);
    }

    public String resolveScoreDuration(FootballDataMatchDto dto) {
        if (dto == null || dto.getScore() == null) {
            return FootballDataScoreNormalizer.DURATION_REGULAR;
        }
        return FootballDataScoreNormalizer.normalizeDuration(dto.getScore().getDuration());
    }

    private static GameResultSideSnapshot sideSnapshot(int externalTeamId, String externalName) {
        return GameResultSideSnapshot.builder()
                .externalId(String.valueOf(externalTeamId))
                .externalName(externalName)
                .build();
    }

    private static LocalDateTime parseUtc(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.endsWith("Z") ? value.substring(0, value.length() - 1) : value;
        return LocalDateTime.parse(normalized, API_DATE);
    }
}
