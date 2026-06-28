package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.fourscore.FourScoreListDates;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Kickoff UTC из wc26_schedule: in-memory map → Mongo → venue+date+time. */
@Component
@RequiredArgsConstructor
public class Wc26ScheduleKickoffResolver {

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;

    public Optional<LocalDateTime> kickoffUtc(int scheduleId) {
        Optional<LocalDateTime> fromMemory = Wc26ScheduleKickoffLookup.kickoffUtc(scheduleId);
        if (fromMemory.isPresent()) {
            return fromMemory;
        }
        return wc26ScheduleMatchRepository.findByScheduleId(scheduleId).flatMap(this::kickoffFromEntity);
    }

    public Optional<LocalDateTime> kickoffForTeamPair(String homeFifa, String awayFifa) {
        if (homeFifa == null || awayFifa == null) {
            return Optional.empty();
        }
        return Wc26ScheduleCatalog.findByTeamPair(homeFifa, awayFifa)
                .flatMap(match -> kickoffUtc(match.scheduleId()));
    }

    /**
     * Все MSK-даты list-страниц 4score/24score для матчей слота (из venue kickoff, не из utcDate записей).
     */
    public Set<LocalDate> listPageDatesForWcSlot(String slotId) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        if (slotId == null || slotId.isBlank()) {
            return dates;
        }
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            kickoffUtc(scheduleId)
                    .ifPresent(utc -> dates.add(FourScoreListDates.listPageDateFromKickoffUtc(utc)));
        }
        return dates;
    }

    Optional<LocalDateTime> kickoffFromEntity(Wc26ScheduleMatch match) {
        if (match == null) {
            return Optional.empty();
        }
        if (match.getKickoffUtc() != null) {
            return Optional.of(match.getKickoffUtc());
        }
        return Optional.ofNullable(Wc26KickoffUtcCalculator.kickoffUtc(
                match.getDate(),
                match.getTimeLocal(),
                match.getVenueKey(),
                match.getStage()));
    }
}
