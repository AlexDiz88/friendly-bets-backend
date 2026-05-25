package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.ExpandedMatchdaySlotDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeagueMatchdayService {

    TournamentFormatExpander tournamentFormatExpander;
    GetEntityService getEntityService;
    BetsRepository betsRepository;
    LeaguesRepository leaguesRepository;

    public List<ExpandedMatchdaySlotDto> expandSlotsForLeague(League league) {
        if (league == null || league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return List.of();
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        return tournamentFormatExpander.expand(format).stream()
                .map(ExpandedMatchdaySlotDto::from)
                .toList();
    }

    /**
     * Value for UI default and bet input: canonical {@code slot.id}, without rewriting stored leagues.
     */
    public String resolveEffectiveCurrentMatchDay(League league) {
        if (league == null) {
            return "1";
        }
        String stored = league.getCurrentMatchDay();
        if (stored == null || stored.isBlank()) {
            return firstSlotId(league).orElse("1");
        }
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return stored;
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);

        Optional<ExpandedMatchdaySlot> byId = tournamentFormatExpander.findBySlotId(format, stored.trim());
        if (byId.isPresent()) {
            return byId.get().getId();
        }
        try {
            int order = Integer.parseInt(stored.trim());
            Optional<ExpandedMatchdaySlot> byOrder = tournamentFormatExpander.findByOrder(format, order);
            if (byOrder.isPresent()) {
                return byOrder.get().getId();
            }
        } catch (NumberFormatException ignored) {
            // legacy non-numeric value — keep as-is if no slot match
        }
        return firstSlotId(league).orElse(stored);
    }

    public void validateMatchDayForLeague(League league, String matchDay) {
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return;
        }
        if (matchDay == null || matchDay.isBlank()) {
            throw new BadRequestException("matchDayRequired");
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        if (tournamentFormatExpander.findBySlotId(format, matchDay.trim()).isEmpty()) {
            throw new BadRequestException("invalidMatchDayForLeague");
        }
    }

    /**
     * Advances {@link League#getCurrentMatchDay()} after a bet is saved.
     * Skips finished seasons (no migration of historical data).
     * Leagues without format keep legacy numeric progression.
     */
    public void updateCurrentMatchDayAfterBet(Season season, League league) {
        if (season == null || league == null) {
            return;
        }
        if (season.getStatus() == Season.Status.FINISHED) {
            return;
        }
        if (season.getPlayers() == null || season.getPlayers().isEmpty()) {
            throw new BadRequestException("noPlayersInSeason");
        }
        if (season.getBetCountPerMatchDay() == null || season.getBetCountPerMatchDay() == 0) {
            throw new BadRequestException("nullOrZeroBetCountPerMatchDay");
        }

        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            updateLegacyNumericMatchDay(season, league);
            return;
        }

        updateFormatBasedMatchDay(season, league);
    }

    private void updateLegacyNumericMatchDay(Season season, League league) {
        int totalBets = betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED);
        int capacityPerSlot = season.getPlayers().size() * season.getBetCountPerMatchDay();
        int computedTour = totalBets / capacityPerSlot + 1;
        String next = String.valueOf(computedTour);
        if (!next.equals(league.getCurrentMatchDay())) {
            league.setCurrentMatchDay(next);
            leaguesRepository.save(league);
        }
    }

    private void updateFormatBasedMatchDay(Season season, League league) {
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);
        if (slots.isEmpty()) {
            return;
        }

        String currentId = resolveEffectiveCurrentMatchDay(league);
        int capacityPerSlot = season.getPlayers().size() * season.getBetCountPerMatchDay();
        int betsOnCurrent = betsRepository.countBySeason_IdAndLeague_IdAndMatchDayAndBetStatusNot(
                season.getId(),
                league.getId(),
                currentId,
                Bet.BetStatus.DELETED
        );

        if (betsOnCurrent < capacityPerSlot) {
            if (!currentId.equals(league.getCurrentMatchDay())) {
                league.setCurrentMatchDay(currentId);
                leaguesRepository.save(league);
            }
            return;
        }

        int currentOrder = tournamentFormatExpander.resolveOrder(format, currentId).orElse(slots.get(0).getOrder());
        Optional<ExpandedMatchdaySlot> nextSlot = slots.stream()
                .filter(s -> s.getOrder() > currentOrder)
                .findFirst();

        String nextId = nextSlot.map(ExpandedMatchdaySlot::getId).orElse(currentId);
        if (!nextId.equals(league.getCurrentMatchDay())) {
            league.setCurrentMatchDay(nextId);
            leaguesRepository.save(league);
        }
    }

    private Optional<String> firstSlotId(League league) {
        return expandSlotsForLeague(league).stream()
                .findFirst()
                .map(ExpandedMatchdaySlotDto::getId);
    }
}
