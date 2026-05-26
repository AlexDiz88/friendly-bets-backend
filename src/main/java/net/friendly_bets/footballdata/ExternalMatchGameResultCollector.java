package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.ExternalMatchRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Собирает {@link GameResult} из кэша {@code external_matches} для матчей с открытыми ставками.
 */
@Component
@RequiredArgsConstructor
public class ExternalMatchGameResultCollector {

    private static final Logger log = LoggerFactory.getLogger(ExternalMatchGameResultCollector.class);

    private final BetsRepository betsRepository;
    private final ExternalMatchRepository externalMatchRepository;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final GetEntityService getEntityService;
    private final TeamAliasResolver teamAliasResolver;

    public List<GameResult> collectForSeason(Season season) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(season.getId(), Bet.BetStatus.OPENED);
        Map<String, GameResult> unique = new LinkedHashMap<>();
        String footballDataSeason = matchdaySupport.resolveFootballDataSeasonYear(season);
        int skippedNoMatch = 0;
        int skippedNoTeams = 0;
        int skippedNoSlot = 0;
        int skippedNoLeague = 0;

        Map<MatchdayCacheKey, MatchdayMatchIndex> matchdayCache = new HashMap<>();

        for (Bet bet : openedBets) {
            String homeTeamId = extractTeamId(bet.getHomeTeam());
            String awayTeamId = extractTeamId(bet.getAwayTeam());
            String leagueId = extractLeagueId(bet);
            if (homeTeamId == null || awayTeamId == null || leagueId == null) {
                skippedNoTeams++;
                continue;
            }

            League league;
            Team homeTeam;
            Team awayTeam;
            try {
                league = getEntityService.getLeagueOrThrow(leagueId);
                homeTeam = getEntityService.getTeamOrThrow(homeTeamId);
                awayTeam = getEntityService.getTeamOrThrow(awayTeamId);
            } catch (Exception e) {
                log.warn("Auto-settle: cannot load bet {} entities: {}", bet.getId(), e.getMessage());
                skippedNoTeams++;
                continue;
            }

            Optional<String> competitionCode = FootballDataCompetitionMapping
                    .toCompetitionCode(league.getLeagueCode());
            if (competitionCode.isEmpty()) {
                skippedNoLeague++;
                continue;
            }

            Optional<Integer> slotOrder = matchdaySupport.resolveSlotOrder(league, bet.getMatchDay());
            if (slotOrder.isEmpty()) {
                log.warn(
                        "Auto-settle: cannot resolve slot for bet {} league {} matchDay '{}'",
                        bet.getId(),
                        league.getLeagueCode(),
                        bet.getMatchDay()
                );
                skippedNoSlot++;
                continue;
            }

            MatchdayCacheKey cacheKey = new MatchdayCacheKey(
                    competitionCode.get(), slotOrder.get(), footballDataSeason);
            MatchdayMatchIndex matchIndex = matchdayCache.computeIfAbsent(
                    cacheKey,
                    key -> MatchdayMatchIndex.from(
                            externalMatchRepository.findByCompetitionCodeAndMatchdayAndSeason(
                                    key.competitionCode(), key.slotOrder(), key.season()),
                            teamAliasResolver)
            );

            Optional<ExternalMatch> match = matchIndex.findSettleableMatch(homeTeam, awayTeam);

            if (match.isEmpty()) {
                log.warn(
                        "Auto-settle: no external match for bet {} ({}/{}/{} teams {}/{}, season={})",
                        bet.getId(),
                        competitionCode.get(),
                        slotOrder.get(),
                        bet.getMatchDay(),
                        homeTeamId,
                        awayTeamId,
                        footballDataSeason
                );
                skippedNoMatch++;
                continue;
            }

            String key = league.getId()
                    + "_" + homeTeamId
                    + "_" + awayTeamId;
            unique.putIfAbsent(key, GameResult.builder()
                    .leagueId(league.getId())
                    .homeTeamId(homeTeamId)
                    .awayTeamId(awayTeamId)
                    .gameScore(match.get().getGameScore())
                    .build());
        }

        if (openedBets.isEmpty()) {
            log.debug("Auto-settle: no OPENED bets for season {}", season.getId());
        } else if (unique.isEmpty()) {
            log.warn(
                    "Auto-settle: {} OPENED bet(s), 0 settleable matches (noTeams={}, noLeague={}, noSlot={}, noMatch={}), season={}",
                    openedBets.size(),
                    skippedNoTeams,
                    skippedNoLeague,
                    skippedNoSlot,
                    skippedNoMatch,
                    footballDataSeason
            );
        } else {
            log.info(
                    "Auto-settle: collected {} match result(s) from {} OPENED bet(s), season={}",
                    unique.size(),
                    openedBets.size(),
                    footballDataSeason
            );
        }

        return new ArrayList<>(unique.values());
    }

    private static String extractTeamId(Team team) {
        if (team == null) {
            return null;
        }
        String teamId = team.getId();
        return teamId == null || teamId.isBlank() ? null : teamId;
    }

    private static String extractLeagueId(Bet bet) {
        if (bet.getLeague() == null) {
            return null;
        }
        String leagueId = bet.getLeague().getId();
        return leagueId == null || leagueId.isBlank() ? null : leagueId;
    }

    private record MatchdayCacheKey(String competitionCode, int slotOrder, String season) {
    }

    private static final class MatchdayMatchIndex {
        private final Map<String, ExternalMatch> byInternalPair;
        private final Map<Long, ExternalMatch> byFootballDataPair;
        private final List<ExternalMatch> settleableMatches;
        private final TeamAliasResolver teamAliasResolver;

        private MatchdayMatchIndex(
                Map<String, ExternalMatch> byInternalPair,
                Map<Long, ExternalMatch> byFootballDataPair,
                List<ExternalMatch> settleableMatches,
                TeamAliasResolver teamAliasResolver
        ) {
            this.byInternalPair = byInternalPair;
            this.byFootballDataPair = byFootballDataPair;
            this.settleableMatches = settleableMatches;
            this.teamAliasResolver = teamAliasResolver;
        }

        static MatchdayMatchIndex from(List<ExternalMatch> matches, TeamAliasResolver teamAliasResolver) {
            Map<String, ExternalMatch> byInternalPair = new LinkedHashMap<>();
            Map<Long, ExternalMatch> byFootballDataPair = new LinkedHashMap<>();
            List<ExternalMatch> settleableMatches = new ArrayList<>();
            for (ExternalMatch match : matches) {
                if (!isSettleable(match)) {
                    continue;
                }
                settleableMatches.add(match);
                if (match.getHomeTeamId() != null && match.getAwayTeamId() != null) {
                    byInternalPair.putIfAbsent(pairKey(match.getHomeTeamId(), match.getAwayTeamId()), match);
                }
                if (match.getHomeFootballDataTeamId() > 0 && match.getAwayFootballDataTeamId() > 0) {
                    byFootballDataPair.putIfAbsent(
                            fdPairKey(match.getHomeFootballDataTeamId(), match.getAwayFootballDataTeamId()),
                            match
                    );
                }
            }
            return new MatchdayMatchIndex(byInternalPair, byFootballDataPair, settleableMatches, teamAliasResolver);
        }

        Optional<ExternalMatch> findSettleableMatch(Team homeTeam, Team awayTeam) {
            ExternalMatch byIds = byInternalPair.get(pairKey(homeTeam.getId(), awayTeam.getId()));
            if (byIds != null) {
                return Optional.of(byIds);
            }

            Optional<Integer> homeFdId = teamAliasResolver.resolveFootballDataTeamId(homeTeam);
            Optional<Integer> awayFdId = teamAliasResolver.resolveFootballDataTeamId(awayTeam);
            if (homeFdId.isPresent() && awayFdId.isPresent()) {
                ExternalMatch byFd = byFootballDataPair.get(fdPairKey(homeFdId.get(), awayFdId.get()));
                if (byFd != null) {
                    return Optional.of(byFd);
                }
            }

            for (ExternalMatch match : settleableMatches) {
                if (teamsMatch(match, homeTeam, awayTeam)) {
                    return Optional.of(match);
                }
            }
            return Optional.empty();
        }

        private boolean teamsMatch(ExternalMatch match, Team homeTeam, Team awayTeam) {
            return teamAliasResolver.teamMatchesFootballDataSide(
                    homeTeam, match.getHomeFootballDataTeamId(), match.getHomeTeamName())
                    && teamAliasResolver.teamMatchesFootballDataSide(
                    awayTeam, match.getAwayFootballDataTeamId(), match.getAwayTeamName());
        }

        private static String pairKey(String homeId, String awayId) {
            return homeId + "_" + awayId;
        }

        private static long fdPairKey(int homeFdId, int awayFdId) {
            return ((long) homeFdId << 32) | (awayFdId & 0xffffffffL);
        }

        private static boolean isSettleable(ExternalMatch match) {
            if (!FootballDataMatchStatuses.isTerminal(match.getStatus())) {
                return false;
            }
            GameScore score = match.getGameScore();
            if (score == null || score.getFullTime() == null || score.getFullTime().isBlank()) {
                return false;
            }
            boolean hasInternalTeams = match.getHomeTeamId() != null && !match.getHomeTeamId().isBlank()
                    && match.getAwayTeamId() != null && !match.getAwayTeamId().isBlank();
            boolean hasExternalTeams = match.getHomeFootballDataTeamId() > 0 && match.getAwayFootballDataTeamId() > 0;
            return hasInternalTeams || hasExternalTeams;
        }
    }
}
