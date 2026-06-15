package net.friendly_bets.twentyfourscore;

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
public class TwentyFourScoreGameResultMapper {

    private final TwentyFourScoreScoreNormalizer scoreNormalizer;

    public GameResultRecord toSecondaryPatch(
            GameResultRecord existing,
            TwentyFourScoreMatchDetails details,
            Team homeTeam,
            Team awayTeam,
            LocalDateTime fetchedAt
    ) {
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(details);
        GameResultSourceSnapshot source = toSourceSnapshot(
                details,
                homeTeam,
                awayTeam,
                existing != null ? existing.getSeason() : null,
                normalized,
                fetchedAt
        );
        return GameResultRecord.builder()
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        source
                ))
                .build();
    }

    public GameResultRecord toSecondaryPatchFromList(
            GameResultRecord existing,
            TwentyFourScoreListMatch listMatch,
            Team homeTeam,
            Team awayTeam,
            LocalDateTime fetchedAt
    ) {
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(listMatch);
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .externalMatchId(listMatch.getExternalMatchId())
                .externalSeason(existing != null ? existing.getSeason() : null)
                .status(normalized != null ? normalized.status() : null)
                .utcDate(resolveKickoff(listMatch))
                .gameScore(normalized != null ? normalized.gameScore() : null)
                .scoreDuration(normalized != null ? normalized.scoreDuration() : null)
                .liveMinuteLabel(normalized != null ? normalized.liveMinuteLabel() : null)
                .home(sideSnapshot(listMatch.getHomeTeamName()))
                .away(sideSnapshot(listMatch.getAwayTeamName()))
                .fetchedAt(fetchedAt)
                .build();
        return GameResultRecord.builder()
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        source
                ))
                .build();
    }

    private GameResultSourceSnapshot toSourceSnapshot(
            TwentyFourScoreMatchDetails details,
            Team homeTeam,
            Team awayTeam,
            String season,
            TwentyFourScoreScoreNormalizer.NormalizedScore normalized,
            LocalDateTime fetchedAt
    ) {
        return GameResultSourceSnapshot.builder()
                .externalMatchId(details.getExternalMatchId())
                .externalSeason(season)
                .status(normalized != null ? normalized.status() : null)
                .utcDate(details.getKickoffAt())
                .gameScore(normalized != null ? normalized.gameScore() : null)
                .scoreDuration(normalized != null ? normalized.scoreDuration() : null)
                .liveMinuteLabel(normalized != null ? normalized.liveMinuteLabel() : null)
                .home(sideSnapshot(details.getHomeTeamName()))
                .away(sideSnapshot(details.getAwayTeamName()))
                .fetchedAt(fetchedAt)
                .build();
    }

    private static LocalDateTime resolveKickoff(TwentyFourScoreListMatch listMatch) {
        if (listMatch.getMatchDate() == null || listMatch.getKickoffTime() == null) {
            return null;
        }
        return LocalDateTime.of(listMatch.getMatchDate(), listMatch.getKickoffTime());
    }

    private static GameResultSideSnapshot sideSnapshot(String externalName) {
        return GameResultSideSnapshot.builder()
                .externalName(externalName)
                .build();
    }
}
