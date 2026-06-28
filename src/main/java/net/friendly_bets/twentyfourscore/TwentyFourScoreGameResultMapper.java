package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.fourscore.FourScoreKickoffUtc;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.wc26.Wc26ScheduleKickoffResolver;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreGameResultMapper {

    private final TwentyFourScoreScoreNormalizer scoreNormalizer;
    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;

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

    public GameResultRecord toNewRecord(
            TwentyFourScoreListMatch listMatch,
            Team homeTeam,
            Team awayTeam,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            LocalDateTime fetchedAt
    ) {
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(listMatch);
        LocalDateTime kickoffAt = resolveWcKickoffUtc(leagueCode, listMatch, homeTeam, awayTeam);
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .externalMatchId(listMatch.getExternalMatchId())
                .externalSeason(season)
                .status(normalized != null ? normalized.status() : null)
                .utcDate(kickoffAt)
                .gameScore(normalized != null ? normalized.gameScore() : null)
                .scoreDuration(normalized != null ? normalized.scoreDuration() : null)
                .liveMinuteLabel(normalized != null ? normalized.liveMinuteLabel() : null)
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
                .provider(MatchDataProviders.TWENTYFOUR_SCORE)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                        source
                ));
        if (normalized != null) {
            builder.status(normalized.status())
                    .gameScore(normalized.gameScore())
                    .scoreDuration(normalized.scoreDuration())
                    .liveMinuteLabel(normalized.liveMinuteLabel());
        }
        if (kickoffAt != null) {
            builder.utcDate(kickoffAt);
        }
        return builder.build();
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
        return resolveKickoffUtc(listMatch);
    }

    /** 24score — время на странице в MSK; в БД храним UTC (как 4score). */
    static LocalDateTime resolveKickoffUtc(TwentyFourScoreListMatch listMatch) {
        if (listMatch.getMatchDate() == null || listMatch.getKickoffTime() == null) {
            return null;
        }
        return FourScoreKickoffUtc.fromMoscowLocal(listMatch.getMatchDate(), listMatch.getKickoffTime());
    }

    /**
     * ЧМ: канонический kickoff из wc26_schedule (venue → UTC), не MSK-время со страницы 24score.
     * Остальные лиги — MSK со списка 24score.
     */
    LocalDateTime resolveWcKickoffUtc(
            String leagueCode,
            TwentyFourScoreListMatch listMatch,
            Team homeTeam,
            Team awayTeam
    ) {
        if ("WC".equals(leagueCode)) {
            String homeFifa = teamFifa(homeTeam);
            String awayFifa = teamFifa(awayTeam);
            if (homeFifa != null && awayFifa != null) {
                Optional<LocalDateTime> fromSchedule = wc26ScheduleKickoffResolver.kickoffForTeamPair(homeFifa, awayFifa);
                if (fromSchedule.isPresent()) {
                    return fromSchedule.get();
                }
            }
        }
        return resolveKickoffUtc(listMatch);
    }

    private static String teamFifa(Team team) {
        if (team == null) {
            return null;
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle())
                .or(() -> Optional.ofNullable(team.getCountry()).flatMap(Wc26TeamCatalog::fifaCodeForKnownName))
                .orElse(null);
    }

    private static GameResultSideSnapshot sideSnapshot(String externalName) {
        return GameResultSideSnapshot.builder()
                .externalName(externalName)
                .build();
    }
}
