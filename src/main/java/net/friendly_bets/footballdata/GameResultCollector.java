package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.gameresults.GameScoreValidator;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
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

@Component
@RequiredArgsConstructor
public class GameResultCollector {

    private static final Logger log = LoggerFactory.getLogger(GameResultCollector.class);

    private final BetsRepository betsRepository;
    private final GameResultRecordRepository gameResultRecordRepository;
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

            if (!FootballDataCompetitionMapping.isSupported(league.getLeagueCode())) {
                skippedNoLeague++;
                continue;
            }
            String leagueCode = league.getLeagueCode().name();

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
                    leagueCode,
                    slotOrder.get(),
                    matchdaySupport.resolveFootballDataSeasonYear(season, league.getLeagueCode()));
            MatchdayMatchIndex matchIndex = matchdayCache.computeIfAbsent(
                    cacheKey,
                    key -> MatchdayMatchIndex.from(
                            gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                                    key.leagueCode(), key.slotOrder(), key.season()),
                            teamAliasResolver)
            );

            Optional<GameResultRecord> match = matchIndex.findSettleableMatch(homeTeam, awayTeam);

            if (match.isEmpty()) {
                log.warn(
                        "Auto-settle: no game result for bet {} ({}/{}/{} teams {}/{}, season={})",
                        bet.getId(),
                        leagueCode,
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

    private record MatchdayCacheKey(String leagueCode, int slotOrder, String season) {
    }

    private static final class MatchdayMatchIndex {
        private final Map<String, GameResultRecord> byInternalPair;
        private final Map<Long, GameResultRecord> byFootballDataPair;
        private final List<GameResultRecord> settleableMatches;
        private final TeamAliasResolver teamAliasResolver;

        private MatchdayMatchIndex(
                Map<String, GameResultRecord> byInternalPair,
                Map<Long, GameResultRecord> byFootballDataPair,
                List<GameResultRecord> settleableMatches,
                TeamAliasResolver teamAliasResolver
        ) {
            this.byInternalPair = byInternalPair;
            this.byFootballDataPair = byFootballDataPair;
            this.settleableMatches = settleableMatches;
            this.teamAliasResolver = teamAliasResolver;
        }

        static MatchdayMatchIndex from(List<GameResultRecord> matches, TeamAliasResolver teamAliasResolver) {
            Map<String, GameResultRecord> byInternalPair = new LinkedHashMap<>();
            Map<Long, GameResultRecord> byFootballDataPair = new LinkedHashMap<>();
            List<GameResultRecord> settleableMatches = new ArrayList<>();
            for (GameResultRecord match : matches) {
                if (!isSettleable(match)) {
                    continue;
                }
                settleableMatches.add(match);
                if (match.getHomeTeamId() != null && match.getAwayTeamId() != null) {
                    byInternalPair.putIfAbsent(pairKey(match.getHomeTeamId(), match.getAwayTeamId()), match);
                }
                int homeFd = sideFdId(match, true);
                int awayFd = sideFdId(match, false);
                if (homeFd > 0 && awayFd > 0) {
                    byFootballDataPair.putIfAbsent(fdPairKey(homeFd, awayFd), match);
                }
            }
            return new MatchdayMatchIndex(byInternalPair, byFootballDataPair, settleableMatches, teamAliasResolver);
        }

        Optional<GameResultRecord> findSettleableMatch(Team homeTeam, Team awayTeam) {
            GameResultRecord byIds = byInternalPair.get(pairKey(homeTeam.getId(), awayTeam.getId()));
            if (byIds != null) {
                return Optional.of(byIds);
            }

            Optional<Integer> homeFdId = teamAliasResolver.resolveFootballDataTeamId(homeTeam);
            Optional<Integer> awayFdId = teamAliasResolver.resolveFootballDataTeamId(awayTeam);
            if (homeFdId.isPresent() && awayFdId.isPresent()) {
                GameResultRecord byFd = byFootballDataPair.get(fdPairKey(homeFdId.get(), awayFdId.get()));
                if (byFd != null) {
                    return Optional.of(byFd);
                }
            }

            for (GameResultRecord match : settleableMatches) {
                if (teamsMatch(match, homeTeam, awayTeam)) {
                    return Optional.of(match);
                }
            }
            return Optional.empty();
        }

        private boolean teamsMatch(GameResultRecord match, Team homeTeam, Team awayTeam) {
            GameResultSourceSnapshot source = match.footballDataSource();
            if (source == null) {
                return false;
            }
            return teamAliasResolver.teamMatchesFootballDataSide(
                    homeTeam, sideFdId(source.getHome()), externalName(source.getHome()))
                    && teamAliasResolver.teamMatchesFootballDataSide(
                    awayTeam, sideFdId(source.getAway()), externalName(source.getAway()));
        }

        private static String pairKey(String homeId, String awayId) {
            return homeId + "_" + awayId;
        }

        private static long fdPairKey(int homeFdId, int awayFdId) {
            return ((long) homeFdId << 32) | (awayFdId & 0xffffffffL);
        }

        private static boolean isSettleable(GameResultRecord match) {
            if (!match.isFinalized()) {
                return false;
            }
            if (!GameScoreValidator.hasValidFullTime(match.getGameScore())) {
                return false;
            }
            GameScore score = match.getGameScore();
            boolean hasInternalTeams = match.getHomeTeamId() != null && !match.getHomeTeamId().isBlank()
                    && match.getAwayTeamId() != null && !match.getAwayTeamId().isBlank();
            boolean hasExternalTeams = sideFdId(match, true) > 0 && sideFdId(match, false) > 0;
            return hasInternalTeams || hasExternalTeams;
        }

        private static int sideFdId(GameResultRecord match, boolean home) {
            GameResultSourceSnapshot source = match.footballDataSource();
            if (source == null) {
                return 0;
            }
            return sideFdId(home ? source.getHome() : source.getAway());
        }

        private static int sideFdId(GameResultSideSnapshot side) {
            if (side == null || side.getExternalId() == null || side.getExternalId().isBlank()) {
                return 0;
            }
            try {
                return Integer.parseInt(side.getExternalId().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static String externalName(GameResultSideSnapshot side) {
            return side != null ? side.getExternalName() : null;
        }
    }
}
