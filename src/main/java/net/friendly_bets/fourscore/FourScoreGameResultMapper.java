package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FourScoreGameResultMapper {

    private final FourScoreScoreNormalizer scoreNormalizer;

    public GameResultRecord toIncomingPatch(
            GameResultRecord existing,
            FourScoreEventDetails details,
            Team homeTeam,
            Team awayTeam,
            Long externalEventId,
            LocalDateTime fetchedAt
    ) {
        FourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(details);
        GameResultSourceSnapshot source = toSourceSnapshot(
                details,
                homeTeam,
                awayTeam,
                externalEventId,
                existing != null ? existing.getSeason() : null,
                normalized,
                fetchedAt
        );

        GameResultRecord.GameResultRecordBuilder builder = GameResultRecord.builder();
        if (existing != null) {
            builder.id(existing.getId())
                    .leagueCode(existing.getLeagueCode())
                    .matchday(existing.getMatchday())
                    .season(existing.getSeason())
                    .leagueId(existing.getLeagueId())
                    .homeTeamId(existing.getHomeTeamId())
                    .awayTeamId(existing.getAwayTeamId());
        } else {
            builder.homeTeamId(homeTeam.getId())
                    .awayTeamId(awayTeam.getId());
        }

        if (normalized != null) {
            builder.status(normalized.status())
                    .gameScore(normalized.gameScore())
                    .scoreDuration(normalized.scoreDuration())
                    .liveMinuteLabel(normalized.liveMinuteLabel());
        }
        if (details.getKickoffAt() != null) {
            builder.utcDate(FourScoreKickoffUtc.fromMoscowLocal(details.getKickoffAt()));
        } else if (existing != null) {
            builder.utcDate(existing.getUtcDate());
        }
        builder.fetchedAt(fetchedAt)
                .provider(MatchDataProviders.FOURSCORE)
                .fourscoreEventSlug(details.getEventSlug())
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        source
                ));
        return builder.build();
    }

    public GameResultRecord toNewRecord(
            FourScoreEventDetails details,
            Team homeTeam,
            Team awayTeam,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            Long externalEventId,
            LocalDateTime fetchedAt
    ) {
        FourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(details);
        GameResultSourceSnapshot source = toSourceSnapshot(
                details,
                homeTeam,
                awayTeam,
                externalEventId,
                season,
                normalized,
                fetchedAt
        );
        GameResultRecord.GameResultRecordBuilder builder = GameResultRecord.builder()
                .leagueCode(leagueCode)
                .matchday(matchday)
                .season(season)
                .leagueId(leagueId)
                .homeTeamId(homeTeam.getId())
                .awayTeamId(awayTeam.getId())
                .fetchedAt(fetchedAt)
                .provider(MatchDataProviders.FOURSCORE)
                .fourscoreEventSlug(details.getEventSlug())
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        source
                ));
        if (normalized != null) {
            builder.status(normalized.status())
                    .gameScore(normalized.gameScore())
                    .scoreDuration(normalized.scoreDuration())
                    .liveMinuteLabel(normalized.liveMinuteLabel());
        }
        if (details.getKickoffAt() != null) {
            builder.utcDate(FourScoreKickoffUtc.fromMoscowLocal(details.getKickoffAt()));
        }
        return builder.build();
    }

    public GameResultRecord toNewRecordFromList(
            FourScoreListMatch listMatch,
            Team homeTeam,
            Team awayTeam,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            Long externalEventId,
            LocalDate listPageDate,
            LocalDateTime fetchedAt
    ) {
        String status = FourScoreScoreNormalizer.mapStatus(listMatch.getStatusText());
        LocalDateTime kickoffAt = resolveListKickoff(listPageDate, listMatch.getKickoffTime());
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .externalMatchId(externalEventId != null ? externalEventId : 0L)
                .externalSeason(season)
                .status(status)
                .utcDate(kickoffAt)
                .home(sideSnapshot(listMatch.getHomeTeamName()))
                .away(sideSnapshot(listMatch.getAwayTeamName()))
                .fetchedAt(fetchedAt)
                .build();
        GameResultRecord.GameResultRecordBuilder builder = GameResultRecord.builder()
                .leagueCode(leagueCode)
                .matchday(matchday)
                .season(season)
                .leagueId(leagueId)
                .homeTeamId(homeTeam.getId())
                .awayTeamId(awayTeam.getId())
                .fetchedAt(fetchedAt)
                .provider(MatchDataProviders.FOURSCORE)
                .status(status)
                .fourscoreEventSlug(listMatch.getEventSlug())
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        source
                ));
        if (kickoffAt != null) {
            builder.utcDate(kickoffAt);
        }
        return builder.build();
    }

    public GameResultRecord toIncomingPatchFromList(
            GameResultRecord existing,
            FourScoreListMatch listMatch,
            Team homeTeam,
            Team awayTeam,
            Long externalEventId,
            LocalDate listPageDate,
            LocalDateTime fetchedAt
    ) {
        String status = FourScoreScoreNormalizer.mapStatus(listMatch.getStatusText());
        LocalDateTime kickoffAt = resolveListKickoff(listPageDate, listMatch.getKickoffTime());
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .externalMatchId(externalEventId != null ? externalEventId : 0L)
                .externalSeason(existing != null ? existing.getSeason() : null)
                .status(status)
                .utcDate(kickoffAt)
                .home(sideSnapshot(listMatch.getHomeTeamName()))
                .away(sideSnapshot(listMatch.getAwayTeamName()))
                .fetchedAt(fetchedAt)
                .build();

        GameResultRecord.GameResultRecordBuilder builder = GameResultRecord.builder();
        if (existing != null) {
            builder.id(existing.getId())
                    .leagueCode(existing.getLeagueCode())
                    .matchday(existing.getMatchday())
                    .season(existing.getSeason())
                    .leagueId(existing.getLeagueId())
                    .homeTeamId(existing.getHomeTeamId())
                    .awayTeamId(existing.getAwayTeamId());
        } else {
            builder.homeTeamId(homeTeam.getId())
                    .awayTeamId(awayTeam.getId());
        }
        builder.status(status)
                .fetchedAt(fetchedAt)
                .provider(MatchDataProviders.FOURSCORE)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        source
                ));
        if (kickoffAt != null) {
            builder.utcDate(kickoffAt);
        } else if (existing != null) {
            builder.utcDate(existing.getUtcDate());
        }
        if (listMatch.getEventSlug() != null && !listMatch.getEventSlug().isBlank()) {
            builder.fourscoreEventSlug(listMatch.getEventSlug());
        }
        return builder.build();
    }

    private static LocalDateTime resolveListKickoff(LocalDate listPageDate, LocalTime kickoffTime) {
        return FourScoreKickoffUtc.fromMoscowLocal(listPageDate, kickoffTime);
    }

    public GameResultSourceSnapshot toSourceSnapshot(
            FourScoreEventDetails details,
            Team homeTeam,
            Team awayTeam,
            Long externalEventId,
            String season,
            FourScoreScoreNormalizer.NormalizedScore normalized,
            LocalDateTime fetchedAt
    ) {
        return GameResultSourceSnapshot.builder()
                .externalMatchId(externalEventId != null ? externalEventId : 0L)
                .externalSeason(season)
                .status(normalized != null ? normalized.status() : null)
                .utcDate(details.getKickoffAt() != null
                        ? FourScoreKickoffUtc.fromMoscowLocal(details.getKickoffAt())
                        : null)
                .gameScore(normalized != null ? normalized.gameScore() : null)
                .scoreDuration(normalized != null ? normalized.scoreDuration() : null)
                .liveMinuteLabel(normalized != null ? normalized.liveMinuteLabel() : null)
                .home(sideSnapshot(details.getHomeTeamName()))
                .away(sideSnapshot(details.getAwayTeamName()))
                .fetchedAt(fetchedAt)
                .build();
    }

    private static GameResultSideSnapshot sideSnapshot(String externalName) {
        return GameResultSideSnapshot.builder()
                .externalName(externalName)
                .build();
    }
}
