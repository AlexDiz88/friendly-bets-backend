package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.dto.PlaceBetFromOddsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.ForbiddenException;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.models.*;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.oddsapi.OddsPresentationService;
import net.friendly_bets.oddsapi.OddsSelectionBetTitleMapper;
import net.friendly_bets.repositories.BetPlacementIdempotencyRepository;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.wc26.Wc26GameResultLinker;
import net.friendly_bets.wc26.Wc26MatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.friendly_bets.utils.BetUtils.checkBetOdds;
import static net.friendly_bets.utils.BetUtils.checkIfBetAlreadyExists;

@Service
@RequiredArgsConstructor
public class SelfBetPlacementService {

    private static final double ODDS_TOLERANCE = 0.01;
    private static final List<Bet.BetStatus> SLOT_BET_STATUSES = Arrays.asList(
            Bet.BetStatus.OPENED,
            Bet.BetStatus.WON,
            Bet.BetStatus.RETURNED,
            Bet.BetStatus.LOST
    );

    private final SeasonsRepository seasonsRepository;
    private final BetsRepository betsRepository;
    private final BetPlacementIdempotencyRepository idempotencyRepository;
    private final GetEntityService getEntityService;
    private final Wc26MatchService wc26MatchService;
    private final Wc26GameResultLinker gameResultLinker;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final OddsPresentationService oddsPresentationService;
    private final LeagueMatchdayService leagueMatchdayService;
    private final CalendarsService calendarsService;
    private final PlayerStatsService playerStatsService;
    private final FootballDataSyncService footballDataSyncService;

    @Transactional
    public BetDto placeFromOdds(String userId, PlaceBetFromOddsDto dto, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<BetPlacementIdempotency> existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                Bet bet = betsRepository.findById(existing.get().getBetId())
                        .orElseThrow(() -> new ConflictException("betNotFound"));
                return BetDto.from(bet);
            }
        }

        Season season = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE)
                .orElseThrow(() -> new BadRequestException("noActiveSeason"));
        if (!isParticipant(season, userId)) {
            throw new ForbiddenException("notSeasonParticipant");
        }

        League wcLeague = wc26MatchService.requireWcLeague(season);
        leagueMatchdayService.validateMatchDayForLeague(wcLeague, dto.getMatchDay());

        String storageSeason = matchdaySupport.resolveFootballDataSeasonYear(season, wcLeague.getLeagueCode());
        GameResultRecord match = gameResultLinker.findByScheduleId(dto.getWc26ScheduleId(), storageSeason)
                .orElseThrow(() -> new BadRequestException("wc26GameResultNotMapped"));

        LocalDateTime now = LocalDateTime.now();
        if (!GameResultNotStarted.isNotStarted(match, now)) {
            throw new BadRequestException("matchAlreadyStarted");
        }
        validateSlotOpen(dto.getMatchDay(), storageSeason, now);

        User user = getEntityService.getUserOrThrow(userId);
        Team homeTeam = getEntityService.getTeamOrThrow(match.getHomeTeamId());
        Team awayTeam = getEntityService.getTeamOrThrow(match.getAwayTeamId());

        NewBetDto probe = NewBetDto.builder()
                .userId(userId)
                .seasonId(season.getId())
                .leagueId(wcLeague.getId())
                .matchDay(dto.getMatchDay())
                .homeTeamId(homeTeam.getId())
                .awayTeamId(awayTeam.getId())
                .betTitle(BetTitle.builder().code((short) 101).label("П1").isNot(false).build())
                .betOdds(1.01)
                .betSize(1)
                .calendarNodeId("x")
                .build();
        checkIfBetAlreadyExists(betsRepository, probe);

        int slotLimit = WcTournamentSlots.betsRequiredForSlot(dto.getMatchDay());
        long slotBets = betsRepository.countBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndBetStatusIn(
                season.getId(),
                wcLeague.getId(),
                userId,
                dto.getMatchDay(),
                SLOT_BET_STATUSES
        );
        if (slotBets >= slotLimit) {
            throw new ConflictException("betSlotLimitReached");
        }

        OddsPresentationService.OddsLineSelection selection = oddsPresentationService
                .findSelection(match.getId(), dto.getSelectionKey(), dto.getBookmaker())
                .orElseThrow(() -> new BadRequestException("oddsSelectionNotFound"));

        double serverOdds = parseOdds(selection.odds());
        if (Math.abs(serverOdds - dto.getClientOdds()) > ODDS_TOLERANCE) {
            throw new ConflictException("oddsChangedRetry");
        }
        checkBetOdds(serverOdds);

        BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(selection.category(), selection.row());
        String calendarNodeId = resolveCalendarNodeId(season.getId(), wcLeague.getLeagueCode().name(), dto.getMatchDay());
        int betSize = resolveBetSize(season, calendarNodeId, wcLeague.getLeagueCode().name(), dto.getMatchDay());

        Bet bet = Bet.builder()
                .createdAt(now)
                .createdBy(user)
                .user(user)
                .season(season)
                .league(wcLeague)
                .matchDay(dto.getMatchDay())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .betTitle(betTitle)
                .betOdds(serverOdds)
                .betSize(betSize)
                .betStatus(Bet.BetStatus.OPENED)
                .calendarNodeId(calendarNodeId)
                .oddsSource(BetOddsSource.ODDS_API)
                .oddsBookmaker(dto.getBookmaker())
                .oddsSelectionKey(dto.getSelectionKey())
                .oddsLockedAt(now)
                .wc26ScheduleId(dto.getWc26ScheduleId())
                .build();

        betsRepository.save(bet);
        leagueMatchdayService.updateCurrentMatchDayAfterBet(season, wcLeague);
        calendarsService.addBetToCalendarNode(bet, calendarNodeId, wcLeague.getId(), dto.getMatchDay());
        playerStatsService.calculateStatsBasedOnNewOpenedBet(season.getId(), wcLeague.getId(), user, true);
        footballDataSyncService.registerPollingForOpenedBet(bet);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRepository.save(BetPlacementIdempotency.builder()
                    .idempotencyKey(idempotencyKey.trim())
                    .betId(bet.getId())
                    .createdAt(now)
                    .build());
        }

        return BetDto.from(bet);
    }

    private void validateSlotOpen(String matchDay, String storageSeason, LocalDateTime now) {
        List<Integer> scheduleIds = WcTournamentSlots.scheduleIdsForSlot(matchDay);
        if (scheduleIds.isEmpty()) {
            return;
        }
        Integer firstId = scheduleIds.get(0);
        GameResultRecord first = gameResultLinker.findByScheduleId(firstId, storageSeason).orElse(null);
        if (first != null && first.getUtcDate() != null && !first.getUtcDate().isAfter(now)) {
            throw new BadRequestException("betSlotClosed");
        }
    }

    private static boolean leagueCodesMatch(League.LeagueCode nodeCode, String leagueCode) {
        if (nodeCode == null || leagueCode == null || leagueCode.isBlank()) {
            return false;
        }
        return nodeCode.name().equalsIgnoreCase(leagueCode.trim());
    }

    private String resolveCalendarNodeId(String seasonId, String leagueCode, String matchDay) {
        List<CalendarNode> nodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(seasonId);
        for (CalendarNode node : nodes) {
            if (node.getLeagueMatchdayNodes() == null) {
                continue;
            }
            for (LeagueMatchdayNode lmn : node.getLeagueMatchdayNodes()) {
                if (leagueCodesMatch(lmn.getLeagueCode(), leagueCode)
                        && matchDay.equals(lmn.getMatchDay())) {
                    return node.getId();
                }
            }
        }
        throw new BadRequestException("calendarNodeNotFound");
    }

    private int resolveBetSize(Season season, String calendarNodeId, String leagueCode, String matchDay) {
        List<CalendarNode> nodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(season.getId());
        for (CalendarNode node : nodes) {
            if (!calendarNodeId.equals(node.getId()) || node.getLeagueMatchdayNodes() == null) {
                continue;
            }
            for (LeagueMatchdayNode lmn : node.getLeagueMatchdayNodes()) {
                if (leagueCodesMatch(lmn.getLeagueCode(), leagueCode)
                        && matchDay.equals(lmn.getMatchDay())
                        && lmn.getDefaultBetSize() != null
                        && lmn.getDefaultBetSize() > 0) {
                    return lmn.getDefaultBetSize();
                }
            }
        }
        if (season.getDefaultBetSize() != null && season.getDefaultBetSize() > 0) {
            return season.getDefaultBetSize();
        }
        throw new BadRequestException("betSizeNotConfigured");
    }

    private static double parseOdds(String raw) {
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BadRequestException("invalidOdds");
        }
    }

    private static boolean isParticipant(Season season, String userId) {
        if (userId == null || season.getPlayers() == null) {
            return false;
        }
        return season.getPlayers().stream()
                .filter(Objects::nonNull)
                .map(User::getId)
                .anyMatch(userId::equals);
    }
}
