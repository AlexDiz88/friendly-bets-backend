package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.GameResultsToMatchSchedulesMigrationResultDto;
import net.friendly_bets.dto.MatchScheduleBetsLinkResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.utils.SeasonCalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.friendly_bets.utils.Constants.MATCH_BET_STATUSES;

@Service
@RequiredArgsConstructor
public class GameResultsToMatchSchedulesMigrationService {

    public static final String DEFAULT_SEASON_ID = "6a25bb7766feb468f3c22f83";
    public static final String DEFAULT_LEAGUE_CODE = "WC";

    private static final Logger log = LoggerFactory.getLogger(GameResultsToMatchSchedulesMigrationService.class);

    private final GameResultRecordRepository gameResultRecordRepository;
    private final MatchScheduleRepository matchScheduleRepository;
    private final BetsRepository betsRepository;
    private final SeasonsRepository seasonsRepository;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;

    @Transactional
    public GameResultsToMatchSchedulesMigrationResultDto migrate(
            String seasonIdRaw,
            String leagueCodeRaw
    ) {
        ResolvedContext ctx = resolveContext(seasonIdRaw, leagueCodeRaw);
        Map<Integer, String> slotIdByOrder = buildSlotIdByOrder(ctx.league());

        List<GameResultRecord> records = gameResultRecordRepository.findByLeagueCodeAndSeason(
                ctx.leagueCode(), ctx.sourceSeasonYear());

        int upserted = 0;
        int skipped = 0;
        int errors = 0;

        for (GameResultRecord record : records) {
            try {
                if (record.getHomeTeamId() == null || record.getHomeTeamId().isBlank()
                        || record.getAwayTeamId() == null || record.getAwayTeamId().isBlank()) {
                    skipped++;
                    continue;
                }

                String slotId = slotIdByOrder.getOrDefault(
                        record.getMatchday(),
                        String.valueOf(record.getMatchday()));
                upsertSchedule(record, ctx.seasonId(), ctx.league(), slotId);
                upserted++;
            } catch (Exception e) {
                errors++;
                log.warn("migrate game_result {} failed: {}", record.getId(), e.getMessage());
            }
        }

        return GameResultsToMatchSchedulesMigrationResultDto.builder()
                .seasonId(ctx.seasonId())
                .leagueCode(ctx.leagueCode())
                .sourceSeasonYear(ctx.sourceSeasonYear())
                .matchesRead(records.size())
                .matchesUpserted(upserted)
                .matchesSkipped(skipped)
                .errors(errors)
                .build();
    }

    /**
     * For each match_schedule in season+league: find matching bets and set {@code match_schedule_id}.
     */
    @Transactional
    public MatchScheduleBetsLinkResultDto linkBetsToSchedules(
            String seasonIdRaw,
            String leagueCodeRaw
    ) {
        ResolvedContext ctx = resolveContext(seasonIdRaw, leagueCodeRaw);
        List<MatchSchedule> schedules = matchScheduleRepository.findByLeagueIdAndSeasonId(
                ctx.league().getId(), ctx.seasonId());

        int schedulesProcessed = 0;
        int betsLinked = 0;
        int errors = 0;

        for (MatchSchedule schedule : schedules) {
            try {
                betsLinked += linkBetsForSchedule(schedule, ctx.seasonId(), ctx.league().getId());
                schedulesProcessed++;
            } catch (Exception e) {
                errors++;
                log.warn("link bets for schedule {} failed: {}", schedule.getId(), e.getMessage());
            }
        }

        return MatchScheduleBetsLinkResultDto.builder()
                .seasonId(ctx.seasonId())
                .leagueCode(ctx.leagueCode())
                .schedulesProcessed(schedulesProcessed)
                .betsLinked(betsLinked)
                .errors(errors)
                .build();
    }

    private ResolvedContext resolveContext(String seasonIdRaw, String leagueCodeRaw) {
        String seasonId = (seasonIdRaw == null || seasonIdRaw.isBlank())
                ? DEFAULT_SEASON_ID
                : seasonIdRaw.trim();
        String leagueCode = (leagueCodeRaw == null || leagueCodeRaw.isBlank())
                ? DEFAULT_LEAGUE_CODE
                : leagueCodeRaw.trim().toUpperCase();

        Season season = seasonsRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("Season", seasonId));
        Integer year = SeasonCalendarUtils.resolveExternalSeasonYear(season.getStartDate());
        if (year == null) {
            throw new BadRequestException("seasonDatesRequired");
        }
        League league = findLeagueInSeason(season, leagueCode)
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        return new ResolvedContext(seasonId, leagueCode, String.valueOf(year), league);
    }

    private MatchSchedule upsertSchedule(
            GameResultRecord record,
            String seasonId,
            League league,
            String slotId
    ) {
        Optional<MatchSchedule> existingByTeams = matchScheduleRepository
                .findByLeagueIdAndSeasonIdAndMatchdayAndHomeTeamIdAndAwayTeamId(
                        league.getId(),
                        seasonId,
                        record.getMatchday(),
                        record.getHomeTeamId(),
                        record.getAwayTeamId()
                );

        MatchSchedule schedule = existingByTeams.orElseGet(() ->
                matchScheduleRepository.findById(record.getId()).orElse(null));

        if (schedule == null) {
            schedule = MatchSchedule.builder()
                    .id(record.getId())
                    .build();
        }

        schedule.setSeasonId(seasonId);
        schedule.setLeagueId(league.getId());
        schedule.setLeagueCode(leagueCodeOrRecord(league, record));
        schedule.setMatchday(record.getMatchday());
        schedule.setSlotId(slotId);
        schedule.setHomeTeamId(record.getHomeTeamId());
        schedule.setAwayTeamId(record.getAwayTeamId());
        schedule.setUtcKickoff(toInstant(record.getUtcDate()));
        schedule.setStatus(record.getStatus());
        schedule.setGameScore(record.getGameScore());
        schedule.setFetchedAt(record.getFetchedAt() != null ? record.getFetchedAt() : LocalDateTime.now());

        return matchScheduleRepository.save(schedule);
    }

    private int linkBetsForSchedule(MatchSchedule schedule, String seasonId, String leagueId) {
        if (schedule.getHomeTeamId() == null || schedule.getAwayTeamId() == null) {
            return 0;
        }
        String slotId = schedule.getSlotId() != null && !schedule.getSlotId().isBlank()
                ? schedule.getSlotId()
                : String.valueOf(schedule.getMatchday());

        Map<String, Bet> byId = new LinkedHashMap<>();
        for (String matchDay : distinctMatchDays(slotId, schedule.getMatchday())) {
            List<Bet> found = betsRepository
                    .findAllBySeason_IdAndLeague_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
                            seasonId,
                            leagueId,
                            matchDay,
                            schedule.getHomeTeamId(),
                            schedule.getAwayTeamId(),
                            MATCH_BET_STATUSES
                    );
            for (Bet bet : found) {
                if (bet.getId() != null && bet.getBetTitle() != null) {
                    byId.put(bet.getId(), bet);
                }
            }
        }

        int linked = 0;
        for (Bet bet : byId.values()) {
            if (Objects.equals(schedule.getId(), bet.getMatchScheduleId())) {
                continue;
            }
            bet.setMatchScheduleId(schedule.getId());
            betsRepository.save(bet);
            linked++;
        }
        return linked;
    }

    private Map<Integer, String> buildSlotIdByOrder(League league) {
        Map<Integer, String> map = new HashMap<>();
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return map;
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        for (ExpandedMatchdaySlot slot : tournamentFormatExpander.expand(format)) {
            map.put(slot.getOrder(), slot.getId());
        }
        return map;
    }

    private static Optional<League> findLeagueInSeason(Season season, String leagueCode) {
        if (season.getLeagues() == null) {
            return Optional.empty();
        }
        return season.getLeagues().stream()
                .filter(Objects::nonNull)
                .filter(l -> l.getLeagueCode() != null && leagueCode.equals(l.getLeagueCode().name()))
                .findFirst();
    }

    private static List<String> distinctMatchDays(String slotId, int matchday) {
        List<String> days = new ArrayList<>();
        if (slotId != null && !slotId.isBlank()) {
            days.add(slotId.trim());
        }
        String asNumber = String.valueOf(matchday);
        if (!days.contains(asNumber)) {
            days.add(asNumber);
        }
        return days;
    }

    private static Instant toInstant(LocalDateTime utcDate) {
        if (utcDate == null) {
            return null;
        }
        return utcDate.toInstant(ZoneOffset.UTC);
    }

    private static String leagueCodeOrRecord(League league, GameResultRecord record) {
        if (league.getLeagueCode() != null) {
            return league.getLeagueCode().name();
        }
        return record.getLeagueCode();
    }

    private record ResolvedContext(
            String seasonId,
            String leagueCode,
            String sourceSeasonYear,
            League league
    ) {
    }
}
