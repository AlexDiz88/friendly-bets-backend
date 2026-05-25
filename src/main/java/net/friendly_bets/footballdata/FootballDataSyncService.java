package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.footballdata.client.FootballDataClient;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchdayResponse;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.models.external.ExternalMatchdaySync;
import net.friendly_bets.models.external.ExternalMatchdaySyncStatus;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.ExternalMatchRepository;
import net.friendly_bets.repositories.ExternalMatchdaySyncRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FootballDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(FootballDataSyncService.class);

    private final FootballDataClient footballDataClient;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final FootballDataProperties properties;
    private final FootballDataMatchMapper matchMapper;
    private final FootballDataTeamResolver teamResolver;
    private final ExternalMatchRepository externalMatchRepository;
    private final ExternalMatchdaySyncRepository externalMatchdaySyncRepository;
    private final BetsRepository betsRepository;
    private final LeaguesRepository leaguesRepository;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final net.friendly_bets.externaldata.ExternalMatchDataFacade externalMatchDataFacade;

    public void registerPollingForSeason(String seasonId) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        Set<MatchdayKey> keys = new LinkedHashSet<>();

        for (Bet bet : openedBets) {
            if (bet.getLeague() == null || bet.getMatchDay() == null) {
                continue;
            }
            buildMatchdayKey(bet.getLeague(), bet.getMatchDay()).ifPresent(keys::add);
        }

        for (MatchdayKey key : keys) {
            ensurePolling(key);
        }
    }

    /**
     * Плановый опрос football-data.org: только текущий тур каждого турнира, если тур ещё не завершён.
     */
    public int syncPollingMatchdays() {
        if (!properties.isSyncEnabled() || !footballDataClient.isConfigured()) {
            return 0;
        }

        String season = properties.getDefaultSeason();
        int synced = 0;

        for (String competitionCode : FootballDataCompetitionMapping.allCompetitionCodes()) {
            try {
                ExternalCompetitionInfoDto info = footballDataCompetitionService.getCompetitionInfo(
                        competitionCode, season);
                int currentMatchday = info.getCurrentMatchday();

                Optional<ExternalMatchdaySync> existing = externalMatchdaySyncRepository
                        .findByCompetitionCodeAndMatchdayAndSeason(competitionCode, currentMatchday, season);

                if (existing.isPresent()
                        && existing.get().getSyncStatus() == ExternalMatchdaySyncStatus.COMPLETED) {
                    continue;
                }

                syncMatchday(competitionCode, currentMatchday, season);
                synced++;
            } catch (Exception e) {
                log.warn("Failed to poll current matchday for {}: {}", competitionCode, e.getMessage());
            }
        }
        return synced;
    }

    public ExternalMatchdaySync syncMatchday(String competitionCode, int slotOrder, String season) {
        return syncMatchday(competitionCode, slotOrder, season, null);
    }

    public ExternalMatchdaySync syncMatchday(String competitionCode, int slotOrder, String season, String friendlyLeagueId) {
        if (!footballDataClient.isConfigured()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }

        FootballDataMatchdayResponse response = fetchResponse(competitionCode, slotOrder, season, friendlyLeagueId);
        if (response == null || response.getMatches() == null) {
            throw new BadRequestException("footballDataEmptyResponse");
        }

        String resolvedSeason = resolveSeason(response, season);
        LocalDateTime now = LocalDateTime.now();
        String storageLeagueId = friendlyLeagueId;
        if (storageLeagueId == null || storageLeagueId.isBlank()) {
            League.LeagueCode leagueCode = FootballDataCompetitionMapping.toLeagueCode(competitionCode)
                    .orElse(null);
            storageLeagueId = resolveSingleLeagueId(leagueCode);
        }

        int finishedCount = 0;
        for (FootballDataMatchDto matchDto : response.getMatches()) {
            Team homeTeam = teamResolver.resolve(
                    matchDto.getHomeTeam().getId(),
                    matchDto.getHomeTeam().getName()
            ).orElse(null);
            Team awayTeam = teamResolver.resolve(
                    matchDto.getAwayTeam().getId(),
                    matchDto.getAwayTeam().getName()
            ).orElse(null);

            ExternalMatch mapped = matchMapper.toEntity(
                    matchDto, competitionCode, resolvedSeason, slotOrder, homeTeam, awayTeam, storageLeagueId, now);

            ExternalMatch toSave = externalMatchRepository
                    .findByCompetitionCodeAndMatchdayAndSeasonAndExternalMatchId(
                            competitionCode, slotOrder, resolvedSeason, matchDto.getId())
                    .orElse(mapped);

            toSave.setExternalMatchId(mapped.getExternalMatchId());
            toSave.setCompetitionCode(mapped.getCompetitionCode());
            toSave.setMatchday(mapped.getMatchday());
            toSave.setSeason(mapped.getSeason());
            toSave.setStatus(mapped.getStatus());
            toSave.setUtcDate(mapped.getUtcDate());
            toSave.setHomeFootballDataTeamId(mapped.getHomeFootballDataTeamId());
            toSave.setAwayFootballDataTeamId(mapped.getAwayFootballDataTeamId());
            toSave.setHomeTeamName(mapped.getHomeTeamName());
            toSave.setAwayTeamName(mapped.getAwayTeamName());
            toSave.setHomeTeamId(mapped.getHomeTeamId());
            toSave.setAwayTeamId(mapped.getAwayTeamId());
            toSave.setLeagueId(mapped.getLeagueId());
            toSave.setGameScore(mapped.getGameScore());
            toSave.setFetchedAt(now);
            toSave.setApiLastUpdated(mapped.getApiLastUpdated());

            externalMatchRepository.save(toSave);

            if (FootballDataMatchStatuses.isTerminal(matchDto.getStatus())) {
                finishedCount++;
            }
        }

        int expected = response.getResultSet() != null && response.getResultSet().getCount() > 0
                ? response.getResultSet().getCount()
                : response.getMatches().size();

        ExternalMatchdaySync sync = externalMatchdaySyncRepository
                .findByCompetitionCodeAndMatchdayAndSeason(competitionCode, slotOrder, resolvedSeason)
                .orElse(ExternalMatchdaySync.builder()
                        .competitionCode(competitionCode)
                        .matchday(slotOrder)
                        .season(resolvedSeason)
                        .syncStatus(ExternalMatchdaySyncStatus.POLLING)
                        .firstFetchedAt(now)
                        .build());

        sync.setExpectedMatchCount(expected);
        sync.setFinishedMatchCount(finishedCount);
        sync.setLastFetchedAt(now);
        if (sync.getFirstFetchedAt() == null) {
            sync.setFirstFetchedAt(now);
        }

        boolean allTerminal = response.getMatches().stream()
                .allMatch(m -> FootballDataMatchStatuses.isTerminal(m.getStatus()));

        if (expected > 0 && allTerminal) {
            sync.setSyncStatus(ExternalMatchdaySyncStatus.COMPLETED);
            sync.setCompletedAt(now);
            sync.setFinishedMatchCount(finishedCount);
        } else {
            sync.setSyncStatus(ExternalMatchdaySyncStatus.POLLING);
            sync.setCompletedAt(null);
        }

        return externalMatchdaySyncRepository.save(sync);
    }

    public List<ExternalMatch> getMatches(String competitionCode, int matchday, String season) {
        return externalMatchRepository.findByCompetitionCodeAndMatchdayAndSeason(competitionCode, matchday, season);
    }

    public List<GameResult> getCachedGameResultsForSeason(String seasonId) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        Map<String, GameResult> unique = new LinkedHashMap<>();

        for (Bet bet : openedBets) {
            if (bet.getHomeTeam() == null || bet.getAwayTeam() == null || bet.getLeague() == null) {
                continue;
            }

            Optional<String> competitionCode = FootballDataCompetitionMapping
                    .toCompetitionCode(bet.getLeague().getLeagueCode());
            if (competitionCode.isEmpty()) {
                continue;
            }

            Optional<Integer> slotOrder = resolveSlotOrder(bet.getLeague(), bet.getMatchDay());
            if (slotOrder.isEmpty()) {
                continue;
            }

            String season = properties.getDefaultSeason();
            externalMatchRepository
                    .findByCompetitionCodeAndMatchdayAndSeasonAndHomeTeamIdAndAwayTeamId(
                            competitionCode.get(),
                            slotOrder.get(),
                            season,
                            bet.getHomeTeam().getId(),
                            bet.getAwayTeam().getId()
                    )
                    .filter(m -> "FINISHED".equals(m.getStatus()))
                    .filter(m -> m.getGameScore() != null && m.getGameScore().getFullTime() != null)
                    .ifPresent(match -> {
                        String key = bet.getLeague().getId() + "_" + bet.getHomeTeam().getId() + "_" + bet.getAwayTeam().getId();
                        unique.putIfAbsent(key, GameResult.builder()
                                .leagueId(bet.getLeague().getId())
                                .homeTeamId(bet.getHomeTeam().getId())
                                .awayTeamId(bet.getAwayTeam().getId())
                                .gameScore(match.getGameScore())
                                .build());
                    });
        }

        return new ArrayList<>(unique.values());
    }

    public void refreshSeason(String seasonId) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        Set<MatchdayKey> keys = new LinkedHashSet<>();
        for (Bet bet : openedBets) {
            if (bet.getLeague() == null || bet.getMatchDay() == null) {
                continue;
            }
            buildMatchdayKey(bet.getLeague(), bet.getMatchDay()).ifPresent(keys::add);
        }
        for (MatchdayKey key : keys) {
            ensurePolling(key);
            if (footballDataClient.isConfigured()) {
                syncMatchday(key.competitionCode(), key.matchday(), key.season(), key.leagueId());
            }
        }
    }

    private FootballDataMatchdayResponse fetchResponse(
            String competitionCode,
            int slotOrder,
            String season,
            String leagueId
    ) {
        if (leagueId != null && !leagueId.isBlank()) {
            League league = getEntityService.getLeagueOrThrow(leagueId);
            if (league.getTournamentFormatId() != null && !league.getTournamentFormatId().isBlank()) {
                TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
                ExpandedMatchdaySlot slot = tournamentFormatExpander.findByOrder(format, slotOrder)
                        .orElseThrow(() -> new BadRequestException("invalidSlotOrder"));
                List<FootballDataMatchDto> matches = externalMatchDataFacade.fetchMatches(
                        net.friendly_bets.externaldata.ExternalMatchFetchRequest.builder()
                                .competitionCode(competitionCode)
                                .season(season)
                                .slot(slot)
                                .leagueId(leagueId)
                                .build()
                );
                FootballDataMatchdayResponse response = new FootballDataMatchdayResponse();
                response.setMatches(matches);
                FootballDataMatchdayResponse.ResultSet resultSet = new FootballDataMatchdayResponse.ResultSet();
                resultSet.setCount(matches.size());
                response.setResultSet(resultSet);
                return response;
            }
        }

        return FootballDataKnockoutMatchdays
                .stageForMatchday(competitionCode, slotOrder)
                .map(stage -> footballDataClient.fetchMatchesByStage(competitionCode, stage, season))
                .orElseGet(() -> footballDataClient.fetchMatchday(competitionCode, slotOrder, season));
    }

    private void ensurePolling(MatchdayKey key) {
        externalMatchdaySyncRepository
                .findByCompetitionCodeAndMatchdayAndSeason(key.competitionCode(), key.matchday(), key.season())
                .ifPresentOrElse(existing -> {
                    if (existing.getSyncStatus() == ExternalMatchdaySyncStatus.COMPLETED) {
                        log.debug("Matchday already completed: {}", key);
                    }
                }, () -> externalMatchdaySyncRepository.save(ExternalMatchdaySync.builder()
                        .competitionCode(key.competitionCode())
                        .matchday(key.matchday())
                        .season(key.season())
                        .syncStatus(ExternalMatchdaySyncStatus.POLLING)
                        .expectedMatchCount(0)
                        .finishedMatchCount(0)
                        .firstFetchedAt(LocalDateTime.now())
                        .lastFetchedAt(null)
                        .build()));
    }

    private Optional<MatchdayKey> buildMatchdayKey(League league, String matchDay) {
        Optional<String> competitionCode = FootballDataCompetitionMapping.toCompetitionCode(league.getLeagueCode());
        if (competitionCode.isEmpty()) {
            return Optional.empty();
        }
        return resolveSlotOrder(league, matchDay)
                .map(order -> new MatchdayKey(
                        competitionCode.get(),
                        order,
                        properties.getDefaultSeason(),
                        league.getId()
                ));
    }

    private Optional<Integer> resolveSlotOrder(League league, String matchDay) {
        if (league.getTournamentFormatId() != null && !league.getTournamentFormatId().isBlank()) {
            try {
                TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
                Optional<Integer> fromFormat = tournamentFormatExpander.resolveOrder(format, matchDay);
                if (fromFormat.isPresent()) {
                    return fromFormat;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve slot for matchDay '{}' league {}: {}", matchDay, league.getId(), e.getMessage());
            }
        }
        try {
            return Optional.of(Integer.parseInt(matchDay));
        } catch (NumberFormatException e) {
            log.warn("Cannot parse matchDay '{}' for league {}", matchDay, league.getLeagueCode());
            return Optional.empty();
        }
    }

    private String resolveSeason(FootballDataMatchdayResponse response, String fallbackSeason) {
        if (response.getFilters() != null && response.getFilters().getSeason() != null) {
            return response.getFilters().getSeason();
        }
        return fallbackSeason;
    }

    /**
     * В Mongo может быть несколько лиг с одним {@link League.LeagueCode} — берём самую раннюю по {@code createdAt}
     * как каноническую и логируем предупреждение.
     */
    private String resolveSingleLeagueId(League.LeagueCode leagueCode) {
        if (leagueCode == null) {
            return null;
        }
        List<League> leagues = leaguesRepository.findAllByLeagueCode(leagueCode);
        if (leagues.isEmpty()) {
            return null;
        }
        if (leagues.size() > 1) {
            leagues.sort(Comparator.comparing(l ->
                    Optional.ofNullable(l.getCreatedAt()).orElse(LocalDateTime.MIN)));
            log.warn(
                    "Multiple leagues with leagueCode={}, using oldest by createdAt id={} (merge duplicates in DB if wrong)",
                    leagueCode,
                    leagues.get(0).getId());
        }
        return leagues.get(0).getId();
    }

    private record MatchdayKey(String competitionCode, int matchday, String season, String leagueId) {
    }
}
