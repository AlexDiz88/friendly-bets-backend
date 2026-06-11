package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
                    .scoreDuration(normalized.scoreDuration());
        }
        if (details.getKickoffAt() != null) {
            builder.utcDate(details.getKickoffAt());
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
                .utcDate(details.getKickoffAt())
                .gameScore(normalized != null ? normalized.gameScore() : null)
                .scoreDuration(normalized != null ? normalized.scoreDuration() : null)
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
