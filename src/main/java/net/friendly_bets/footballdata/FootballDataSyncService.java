package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.api_football.ApiFootballSecondarySyncService;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.client.FootballDataClient;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchdayResponse;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.gameresults.GameResultFinalizer;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultStabilizationService;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.models.*;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import net.friendly_bets.wc26.Wc26ScheduleLinker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootballDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(FootballDataSyncService.class);

    private final FootballDataClient footballDataClient;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final FootballDataProperties properties;
    private final GameResultMapper gameResultMapper;
    private final GameResultPersistence gameResultPersistence;
    private final GameResultFinalizer gameResultFinalizer;
    private final FootballDataTeamResolver teamResolver;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final BetsRepository betsRepository;
    private final LeaguesRepository leaguesRepository;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final net.friendly_bets.externaldata.ExternalMatchDataFacade externalMatchDataFacade;
    private final GameResultCollector gameResultCollector;
    private final TeamsRepository teamsRepository;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final ApiSyncIssueService apiSyncIssueService;
    private final ApiFootballSecondarySyncService apiFootballSecondarySyncService;
    private final MatchResultStabilizationService stabilizationService;
    private final MatchResultSyncSettingsService matchResultSyncSettingsService;
    private final Wc26ScheduleLinker wc26ScheduleLinker;

    public void registerPollingForSeason(String seasonId) {
        getEntityService.getSeasonOrThrow(seasonId);
        collectMatchdayKeysForSeasonId(seasonId).forEach(this::ensurePolling);
    }

    /**
     * Регистрирует опрос тура при новой открытой ставке.
     */
    public void registerPollingForOpenedBet(Bet bet) {
        if (bet.getBetStatus() != Bet.BetStatus.OPENED || bet.getMatchDay() == null || bet.getMatchDay().isBlank()) {
            return;
        }
        String leagueId = extractLeagueId(bet);
        String seasonId = extractSeasonId(bet);
        if (leagueId == null || seasonId == null) {
            return;
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        matchdaySupport.buildMatchdayKey(
                        league,
                        bet.getMatchDay(),
                        matchdaySupport.resolveFootballDataSeasonYear(season, league.getLeagueCode()))
                .ifPresent(key -> {
                    ensurePolling(key);
                    syncMatchdayIfConfigured(key);
                });
    }

    private void syncMatchdayIfConfigured(FootballDataMatchdayKey key) {
        if (!properties.isSyncEnabled() || !footballDataClient.isConfigured()) {
            return;
        }
        try {
            syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
        } catch (Exception e) {
            log.warn("Immediate matchday sync failed for {}: {}", key, e.getMessage());
        }
    }

    /**
     * Плановый tick: опрос туров с открытыми ставками + текущих туров API (если ещё не COMPLETED).
     */
    public int runPollingTick() {
        if (!properties.isSyncEnabled() || !footballDataClient.isConfigured()) {
            return 0;
        }

        Set<FootballDataMatchdayKey> keys = new LinkedHashSet<>();
        runningSeasonLookup.findRunningSeason().ifPresent(season -> {
            keys.addAll(collectMatchdayKeysForSeason(season));
            addCurrentApiMatchdayKeys(keys, season);
        });
        keys.forEach(this::ensurePolling);

        int synced = 0;
        for (FootballDataMatchdayKey key : keys) {
            try {
                syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
                synced++;
            } catch (Exception e) {
                log.warn("Failed to poll matchday {}: {}", key, e.getMessage());
            }
        }
        if (synced > 0) {
            log.debug("Football-data polled {} matchday(s)", synced);
        }
        return synced;
    }

    public GameResultsSync syncMatchday(String pathLeagueOrCompetitionCode, int slotOrder, String season) {
        return syncMatchday(pathLeagueOrCompetitionCode, slotOrder, season, null);
    }

    public GameResultsSync syncMatchday(
            String pathLeagueOrCompetitionCode,
            int slotOrder,
            String season,
            String friendlyLeagueId
    ) {
        if (!footballDataClient.isConfigured()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }

        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);
        String externalCompetitionCode = LeagueCodePathSupport.toExternalCompetitionCode(leagueCode);

        String resolvedSeason = resolveStorageSeason(season);

        FootballDataMatchdayResponse response = fetchResponse(
                externalCompetitionCode, slotOrder, resolvedSeason, friendlyLeagueId);
        if (response == null || response.getMatches() == null) {
            throw new BadRequestException("footballDataEmptyResponse");
        }

        String storageSeason = resolvedSeason;
        LocalDateTime now = GameResultNotStarted.nowUtc();
        String storageLeagueId = friendlyLeagueId;
        if (storageLeagueId == null || storageLeagueId.isBlank()) {
            storageLeagueId = resolveSingleLeagueId(League.LeagueCode.valueOf(leagueCode));
        }

        int finishedCount = 0;
        boolean hadUnmappedTeams = false;
        for (FootballDataMatchDto matchDto : response.getMatches()) {
            Team homeTeam = teamResolver.resolve(
                    matchDto.getHomeTeam().getId(),
                    matchDto.getHomeTeam().getName()
            ).orElse(null);
            Team awayTeam = teamResolver.resolve(
                    matchDto.getAwayTeam().getId(),
                    matchDto.getAwayTeam().getName()
            ).orElse(null);

            if (homeTeam == null || awayTeam == null) {
                hadUnmappedTeams = true;
                apiSyncIssueService.recordMissingTeamMapping(leagueCode, storageSeason, slotOrder, matchDto);
                log.warn(
                        "Game result skipped (missing team mapping): provider=football-data league={} season={} matchday={} matchId={} home='{}' homeMapped={} away='{}' awayMapped={}",
                        leagueCode,
                        storageSeason,
                        slotOrder,
                        matchDto.getId(),
                        matchDto.getHomeTeam() != null ? matchDto.getHomeTeam().getName() : null,
                        homeTeam != null,
                        matchDto.getAwayTeam() != null ? matchDto.getAwayTeam().getName() : null,
                        awayTeam != null
                );
                continue;
            }

            GameResultRecord incoming = gameResultMapper.toNewRecord(
                    matchDto,
                    leagueCode,
                    storageSeason,
                    slotOrder,
                    homeTeam,
                    awayTeam,
                    storageLeagueId,
                    externalCompetitionCode,
                    now
            );
            apiFootballSecondarySyncService.enrichIncoming(
                    incoming, homeTeam, awayTeam, leagueCode, storageSeason, now);

            Optional<GameResultRecord> existing = gameResultRecordRepository
                    .findByLeagueCodeAndMatchdayAndSeasonAndHomeTeamIdAndAwayTeamId(
                            leagueCode,
                            slotOrder,
                            storageSeason,
                            homeTeam.getId(),
                            awayTeam.getId()
                    );

            if (existing.isPresent()) {
                GameResultRecord record = existing.get();
                if (gameResultPersistence.isLockedAgainstApiSync(record)) {
                    continue;
                }
                gameResultPersistence.applySync(record, incoming, now);
                wc26ScheduleLinker.linkIfNeeded(record, homeTeam, awayTeam);
                gameResultRecordRepository.save(record);
                if (record.isFinalized()) {
                    finishedCount++;
                }
            } else {
                stripCanonicalIfSecondaryProvider(incoming);
                stabilizationService.updateStabilityCounters(incoming, now);
                gameResultFinalizer.tryFinalize(incoming, now);
                wc26ScheduleLinker.linkIfNeeded(incoming, homeTeam, awayTeam);
                gameResultRecordRepository.save(incoming);
                if (incoming.isFinalized()) {
                    finishedCount++;
                }
            }
        }

        int expected = resolveExpectedMatchCount(slotOrder, friendlyLeagueId, response);

        GameResultsSync sync = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, slotOrder, storageSeason)
                .orElse(GameResultsSync.builder()
                        .leagueCode(leagueCode)
                        .matchday(slotOrder)
                        .season(storageSeason)
                        .syncStatus(GameResultsSyncStatus.POLLING)
                        .firstFetchedAt(now)
                        .build());

        Optional<String> berlinSlotId = resolveSlotId(friendlyLeagueId, slotOrder)
                .filter(WcBerlinSlotMatchFilter::isBerlinGroupSlot);
        if (berlinSlotId.isPresent()) {
            String slotId = berlinSlotId.get();
            expected = WcBerlinSlotMatchFilter.expectedMatchCount(slotId);
            List<GameResultRecord> slotRecords = WcBerlinSlotMatchFilter.filterGameResultRecords(
                    slotId,
                    gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                            leagueCode, slotOrder, storageSeason),
                    teamId -> {
                        if (teamId == null || teamId.isBlank()) {
                            return Optional.empty();
                        }
                        return teamsRepository.findById(teamId);
                    });
            finishedCount = (int) slotRecords.stream().filter(GameResultRecord::isFinalized).count();
        }

        sync.setExpectedMatchCount(expected);
        sync.setFinishedMatchCount(finishedCount);
        sync.setLastFetchedAt(now);
        if (sync.getFirstFetchedAt() == null) {
            sync.setFirstFetchedAt(now);
        }

        boolean allTerminal = !hadUnmappedTeams && response.getMatches().stream()
                .allMatch(m -> FootballDataMatchStatuses.isTerminal(m.getStatus()));
        boolean matchdayComplete = berlinSlotId.isPresent()
                ? expected > 0 && finishedCount >= expected
                : expected > 0 && allTerminal;

        if (matchdayComplete) {
            sync.setSyncStatus(GameResultsSyncStatus.COMPLETED);
            sync.setCompletedAt(now);
            sync.setFinishedMatchCount(finishedCount);
        } else {
            sync.setSyncStatus(GameResultsSyncStatus.POLLING);
            sync.setCompletedAt(null);
        }

        return gameResultsSyncRepository.save(sync);
    }

    public List<GameResultRecord> getMatches(String pathLeagueOrCompetitionCode, int matchday, String season) {
        return getMatches(pathLeagueOrCompetitionCode, matchday, season, null);
    }

    public List<GameResultRecord> getMatches(
            String pathLeagueOrCompetitionCode,
            int matchday,
            String season,
            String leagueId
    ) {
        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                leagueCode, matchday, season);
        return applyBerlinFilterIfNeeded(matches, leagueId, matchday);
    }

    public List<GameResult> getCachedGameResultsForSeason(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        return gameResultCollector.collectForSeason(season);
    }

    public void refreshSeason(String seasonId) {
        Set<FootballDataMatchdayKey> keys = collectMatchdayKeysForSeasonId(seasonId);
        for (FootballDataMatchdayKey key : keys) {
            ensurePolling(key);
            if (footballDataClient.isConfigured()) {
                syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
            }
        }
    }

    private Set<FootballDataMatchdayKey> collectMatchdayKeysForSeasonId(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        return collectMatchdayKeysForSeason(season);
    }

    private Set<FootballDataMatchdayKey> collectMatchdayKeysForSeason(Season season) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(season.getId(), Bet.BetStatus.OPENED);
        Set<FootballDataMatchdayKey> keys = new LinkedHashSet<>();
        int skipped = 0;

        for (Bet bet : openedBets) {
            if (bet.getMatchDay() == null || bet.getMatchDay().isBlank()) {
                skipped++;
                continue;
            }
            String leagueId = extractLeagueId(bet);
            if (leagueId == null) {
                skipped++;
                continue;
            }
            try {
                League league = getEntityService.getLeagueOrThrow(leagueId);
                String footballDataSeason = matchdaySupport.resolveFootballDataSeasonYear(season, league.getLeagueCode());
                Optional<FootballDataMatchdayKey> matchdayKey = matchdaySupport.buildMatchdayKey(
                        league, bet.getMatchDay(), footballDataSeason);
                if (matchdayKey.isPresent()) {
                    keys.add(matchdayKey.get());
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.warn(
                        "Football-data polling: skip bet {} (league {}): {}",
                        bet.getId(),
                        leagueId,
                        e.getMessage()
                );
                skipped++;
            }
        }

        if (!openedBets.isEmpty()) {
            log.info(
                    "Football-data polling keys from {} OPENED bet(s): {} matchday(s), {} skipped",
                    openedBets.size(),
                    keys.size(),
                    skipped
            );
        }
        return keys;
    }

    /**
     * Дополнительно опрашивает текущий тур API только для лиг, где уже есть открытые ставки
     * (не дергаем EC/WC и прочие коды без активных ставок).
     */
    private void addCurrentApiMatchdayKeys(Set<FootballDataMatchdayKey> keys, Season season) {
        Set<String> leagueCodes = keys.stream()
                .map(FootballDataMatchdayKey::leagueCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leagueCodes.isEmpty()) {
            return;
        }

        for (String leagueCode : leagueCodes) {
            try {
                League.LeagueCode code = League.LeagueCode.valueOf(leagueCode);
                String footballDataSeason = matchdaySupport.resolveFootballDataSeasonYear(season, code);
                String externalCompetitionCode = LeagueCodePathSupport.toExternalCompetitionCode(leagueCode);
                ExternalCompetitionInfoDto info = footballDataCompetitionService.getCompetitionInfo(
                        externalCompetitionCode, footballDataSeason);
                int currentMatchday = info.getCurrentMatchday();

                Optional<GameResultsSync> existing = gameResultsSyncRepository
                        .findByLeagueCodeAndMatchdayAndSeason(leagueCode, currentMatchday, footballDataSeason);

                if (existing.isPresent()
                        && existing.get().getSyncStatus() == GameResultsSyncStatus.COMPLETED) {
                    continue;
                }

                keys.add(new FootballDataMatchdayKey(leagueCode, currentMatchday, footballDataSeason, null));
            } catch (Exception e) {
                log.warn("Failed to resolve current matchday for {}: {}", leagueCode, e.getMessage());
            }
        }
    }

    private static String extractLeagueId(Bet bet) {
        if (bet.getLeague() == null) {
            return null;
        }
        String leagueId = bet.getLeague().getId();
        return leagueId == null || leagueId.isBlank() ? null : leagueId;
    }

    private static String extractSeasonId(Bet bet) {
        if (bet.getSeason() == null) {
            return null;
        }
        String seasonId = bet.getSeason().getId();
        return seasonId == null || seasonId.isBlank() ? null : seasonId;
    }

    private List<GameResultRecord> applyBerlinFilterIfNeeded(
            List<GameResultRecord> matches,
            String leagueId,
            int slotOrder
    ) {
        return resolveSlotId(leagueId, slotOrder)
                .filter(WcBerlinSlotMatchFilter::isBerlinGroupSlot)
                .map(slotId -> WcBerlinSlotMatchFilter.filterGameResultRecords(
                        slotId,
                        matches,
                        teamId -> {
                            if (teamId == null || teamId.isBlank()) {
                                return Optional.empty();
                            }
                            return teamsRepository.findById(teamId);
                        }))
                .orElse(matches);
    }

    private int resolveExpectedMatchCount(
            int slotOrder,
            String leagueId,
            FootballDataMatchdayResponse response
    ) {
        Optional<String> slotId = resolveSlotId(leagueId, slotOrder);
        if (slotId.isPresent() && WcBerlinSlotMatchFilter.isBerlinGroupSlot(slotId.get())) {
            return WcBerlinSlotMatchFilter.expectedMatchCount(slotId.get());
        }
        if (response.getResultSet() != null && response.getResultSet().getCount() > 0) {
            return response.getResultSet().getCount();
        }
        return response.getMatches() != null ? response.getMatches().size() : 0;
    }

    private Optional<String> resolveSlotId(String leagueId, int slotOrder) {
        if (leagueId == null || leagueId.isBlank()) {
            return Optional.empty();
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return Optional.empty();
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        return tournamentFormatExpander.findByOrder(format, slotOrder).map(ExpandedMatchdaySlot::getId);
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
                log.info(
                        "football-data sync via format: competition={} slotOrder={} slotId={} season={} leagueId={}",
                        competitionCode,
                        slotOrder,
                        slot.getId(),
                        season,
                        leagueId
                );
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
                log.info("football-data sync result: {} matches saved for slotOrder={}", matches.size(), slotOrder);
                return response;
            }
        }

        return FootballDataKnockoutMatchdays
                .knockoutSlotForMatchday(competitionCode, slotOrder)
                .map(slot -> fetchKnockoutStage(competitionCode, slot, season))
                .orElseGet(() -> footballDataClient.fetchMatchday(competitionCode, slotOrder, season));
    }

    private FootballDataMatchdayResponse fetchKnockoutStage(
            String competitionCode,
            FootballDataKnockoutMatchdays.KnockoutSlot slot,
            String season
    ) {
        FootballDataMatchdayResponse response = footballDataClient.fetchMatchesByStage(
                competitionCode, slot.stage(), season);
        if (slot.leg() == null || response == null || response.getMatches() == null) {
            return response;
        }
        List<FootballDataMatchDto> filtered = FootballDataLegFilter.filterByLeg(response.getMatches(), slot.leg());
        FootballDataMatchdayResponse narrowed = new FootballDataMatchdayResponse();
        narrowed.setMatches(filtered);
        narrowed.setCompetition(response.getCompetition());
        narrowed.setFilters(response.getFilters());
        FootballDataMatchdayResponse.ResultSet resultSet = new FootballDataMatchdayResponse.ResultSet();
        resultSet.setCount(filtered.size());
        narrowed.setResultSet(resultSet);
        return narrowed;
    }

    private void ensurePolling(FootballDataMatchdayKey key) {
        gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(key.leagueCode(), key.matchday(), key.season())
                .ifPresentOrElse(existing -> {
                    if (existing.getSyncStatus() == GameResultsSyncStatus.COMPLETED) {
                        existing.setSyncStatus(GameResultsSyncStatus.POLLING);
                        existing.setCompletedAt(null);
                        gameResultsSyncRepository.save(existing);
                        log.debug("Reopened polling for matchday (new opened bets): {}", key);
                    }
                }, () -> gameResultsSyncRepository.save(GameResultsSync.builder()
                        .leagueCode(key.leagueCode())
                        .matchday(key.matchday())
                        .season(key.season())
                        .syncStatus(GameResultsSyncStatus.POLLING)
                        .expectedMatchCount(0)
                        .finishedMatchCount(0)
                        .firstFetchedAt(GameResultNotStarted.nowUtc())
                        .lastFetchedAt(null)
                        .build()));
    }

    private String resolveStorageSeason(String requestedSeason) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        return properties.getDefaultSeason();
    }

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

    private void stripCanonicalIfSecondaryProvider(GameResultRecord incoming) {
        String primary = matchResultSyncSettingsService.getEffective().getPrimaryProvider();
        if (MatchDataProviders.FOOTBALL_DATA.equals(primary)) {
            return;
        }
        incoming.setGameScore(null);
        incoming.setScoreDuration(null);
        incoming.setFinalizedAt(null);
        incoming.setFinalizedSource(null);
        incoming.setProvider(primary);
    }
}
