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
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
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
import net.friendly_bets.repositories.SeasonsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SeasonsRepository seasonsRepository;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final net.friendly_bets.externaldata.ExternalMatchDataFacade externalMatchDataFacade;
    private final ExternalMatchGameResultCollector gameResultCollector;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final ExternalSyncIssueService externalSyncIssueService;

    public void registerPollingForSeason(String seasonId) {
        getEntityService.getSeasonOrThrow(seasonId);
        collectMatchdayKeysForSeasonId(seasonId).forEach(this::ensurePolling);
    }

    /** Регистрирует опрос тура при новой открытой ставке. */
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
                        matchdaySupport.resolveFootballDataSeasonYear(season))
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
            syncMatchday(key.competitionCode(), key.matchday(), key.season(), key.leagueId());
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
        seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE).ifPresent(season -> {
            keys.addAll(collectMatchdayKeysForSeason(season));
            addCurrentApiMatchdayKeys(keys, matchdaySupport.resolveFootballDataSeasonYear(season));
        });
        keys.forEach(this::ensurePolling);

        int synced = 0;
        for (FootballDataMatchdayKey key : keys) {
            try {
                syncMatchday(key.competitionCode(), key.matchday(), key.season(), key.leagueId());
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

        String storageSeason = resolveStorageSeason(season);
        LocalDateTime now = LocalDateTime.now();
        String storageLeagueId = friendlyLeagueId;
        if (storageLeagueId == null || storageLeagueId.isBlank()) {
            League.LeagueCode leagueCode = FootballDataCompetitionMapping.toLeagueCode(competitionCode)
                    .orElse(null);
            storageLeagueId = resolveSingleLeagueId(leagueCode);
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

            // Strict mode: only persist matches when both teams are mapped to our Team titles.
            if (homeTeam == null || awayTeam == null) {
                hadUnmappedTeams = true;
                externalSyncIssueService.recordMissingTeamMapping(competitionCode, season, slotOrder, matchDto);
                log.warn(
                        "External match skipped (missing team mapping): provider=football-data competition={} season={} matchday={} matchId={} home='{}' away='{}'",
                        competitionCode,
                        season,
                        slotOrder,
                        matchDto.getId(),
                        matchDto.getHomeTeam() != null ? matchDto.getHomeTeam().getName() : null,
                        matchDto.getAwayTeam() != null ? matchDto.getAwayTeam().getName() : null
                );
                continue;
            }

            Optional<ExternalMatch> existingMatch = externalMatchRepository
                    .findByCompetitionCodeAndMatchdayAndSeasonAndExternalMatchId(
                            competitionCode, slotOrder, storageSeason, matchDto.getId());

            if (existingMatch.isPresent()
                    && FootballDataMatchStatuses.isTerminal(existingMatch.get().getStatus())) {
                ExternalMatch terminal = existingMatch.get();
                GameScore refreshedScore = matchMapper.toGameScore(matchDto);
                boolean needsSave = false;
                if (homeTeam != null && !java.util.Objects.equals(homeTeam.getId(), terminal.getHomeTeamId())) {
                    terminal.setHomeTeamId(homeTeam.getId());
                    needsSave = true;
                }
                if (awayTeam != null && !java.util.Objects.equals(awayTeam.getId(), terminal.getAwayTeamId())) {
                    terminal.setAwayTeamId(awayTeam.getId());
                    needsSave = true;
                }
                if (refreshedScore != null
                        && (terminal.getGameScore() == null
                        || terminal.getGameScore().getFullTime() == null)) {
                    terminal.setGameScore(refreshedScore);
                    needsSave = true;
                }
                if (needsSave) {
                    terminal.setFetchedAt(now);
                    externalMatchRepository.save(terminal);
                    log.info(
                            "Backfilled external match {}: homeTeamId={}, awayTeamId={}",
                            matchDto.getId(),
                            terminal.getHomeTeamId(),
                            terminal.getAwayTeamId()
                    );
                }
                if (FootballDataMatchStatuses.isTerminal(matchDto.getStatus())) {
                    finishedCount++;
                }
                continue;
            }

            ExternalMatch mapped = matchMapper.toEntity(
                    matchDto, competitionCode, storageSeason, slotOrder, homeTeam, awayTeam, storageLeagueId, now);

            ExternalMatch toSave = existingMatch.orElse(mapped);

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
                .findByCompetitionCodeAndMatchdayAndSeason(competitionCode, slotOrder, storageSeason)
                .orElse(ExternalMatchdaySync.builder()
                        .competitionCode(competitionCode)
                        .matchday(slotOrder)
                        .season(storageSeason)
                        .syncStatus(ExternalMatchdaySyncStatus.POLLING)
                        .firstFetchedAt(now)
                        .build());

        sync.setExpectedMatchCount(expected);
        sync.setFinishedMatchCount(finishedCount);
        sync.setLastFetchedAt(now);
        if (sync.getFirstFetchedAt() == null) {
            sync.setFirstFetchedAt(now);
        }

        boolean allTerminal = !hadUnmappedTeams && response.getMatches().stream()
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
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        return gameResultCollector.collectForSeason(season);
    }

    public void refreshSeason(String seasonId) {
        Set<FootballDataMatchdayKey> keys = collectMatchdayKeysForSeasonId(seasonId);
        for (FootballDataMatchdayKey key : keys) {
            ensurePolling(key);
            if (footballDataClient.isConfigured()) {
                syncMatchday(key.competitionCode(), key.matchday(), key.season(), key.leagueId());
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
        String footballDataSeason = matchdaySupport.resolveFootballDataSeasonYear(season);
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
                    "Football-data polling keys from {} OPENED bet(s): {} matchday(s), {} skipped, season={}",
                    openedBets.size(),
                    keys.size(),
                    skipped,
                    footballDataSeason
            );
        }
        return keys;
    }

    /**
     * Дополнительно опрашивает текущий тур API только для лиг, где уже есть открытые ставки
     * (не дергаем EC/WC и прочие коды без активных ставок).
     */
    private void addCurrentApiMatchdayKeys(Set<FootballDataMatchdayKey> keys, String footballDataSeason) {
        Set<String> competitionCodes = keys.stream()
                .map(FootballDataMatchdayKey::competitionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (competitionCodes.isEmpty()) {
            return;
        }

        for (String competitionCode : competitionCodes) {
            try {
                ExternalCompetitionInfoDto info = footballDataCompetitionService.getCompetitionInfo(
                        competitionCode, footballDataSeason);
                int currentMatchday = info.getCurrentMatchday();

                Optional<ExternalMatchdaySync> existing = externalMatchdaySyncRepository
                        .findByCompetitionCodeAndMatchdayAndSeason(
                                competitionCode, currentMatchday, footballDataSeason);

                if (existing.isPresent()
                        && existing.get().getSyncStatus() == ExternalMatchdaySyncStatus.COMPLETED) {
                    continue;
                }

                keys.add(new FootballDataMatchdayKey(competitionCode, currentMatchday, footballDataSeason, null));
            } catch (Exception e) {
                log.warn("Failed to resolve current matchday for {}: {}", competitionCode, e.getMessage());
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
        externalMatchdaySyncRepository
                .findByCompetitionCodeAndMatchdayAndSeason(key.competitionCode(), key.matchday(), key.season())
                .ifPresentOrElse(existing -> {
                    if (existing.getSyncStatus() == ExternalMatchdaySyncStatus.COMPLETED) {
                        existing.setSyncStatus(ExternalMatchdaySyncStatus.POLLING);
                        existing.setCompletedAt(null);
                        externalMatchdaySyncRepository.save(existing);
                        log.debug("Reopened polling for matchday (new opened bets): {}", key);
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
}
