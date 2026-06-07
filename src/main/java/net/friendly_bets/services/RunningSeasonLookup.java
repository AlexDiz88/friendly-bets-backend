package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.SeasonsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Текущий идущий сезон: ACTIVE (регистрация закрыта) или SCHEDULED (регистрация открыта).
 * Одновременно может существовать только один такой сезон.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RunningSeasonLookup {

    SeasonsRepository seasonsRepository;

    public Optional<Season> findRunningSeason() {
        Optional<Season> active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (active.isPresent()) {
            return active;
        }
        return seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
    }

    public Season findRunningSeasonOrThrow(String errorKey) {
        return findRunningSeason()
                .orElseThrow(() -> new BadRequestException(errorKey));
    }

    @Transactional
    public void pauseOtherRunningSeasons(String keepSeasonId) {
        pauseSeasonByStatusIfNot(Season.Status.ACTIVE, keepSeasonId);
        pauseSeasonByStatusIfNot(Season.Status.SCHEDULED, keepSeasonId);
    }

    private void pauseSeasonByStatusIfNot(Season.Status status, String keepSeasonId) {
        seasonsRepository.findSeasonByStatus(status).ifPresent(season -> {
            if (!season.getId().equals(keepSeasonId)) {
                season.setStatus(Season.Status.PAUSED);
                seasonsRepository.save(season);
            }
        });
    }
}
