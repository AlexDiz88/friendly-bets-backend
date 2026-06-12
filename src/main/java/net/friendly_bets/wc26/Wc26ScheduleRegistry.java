package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Loads wc26_schedule into memory for fast lookup (bet slots, Berlin filter). */
@Component
@Order(50)
@RequiredArgsConstructor
public class Wc26ScheduleRegistry implements ApplicationRunner {

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;

    @Override
    public void run(ApplicationArguments args) {
        Map<Integer, Wc26ScheduleCatalog.GroupMatch> loaded = new HashMap<>();
        for (Wc26ScheduleMatch match : wc26ScheduleMatchRepository.findAllByOrderByKickoffUtcAsc()) {
            if (match.getHomeFifa() != null && match.getAwayFifa() != null) {
                loaded.put(
                        match.getScheduleId(),
                        new Wc26ScheduleCatalog.GroupMatch(
                                match.getScheduleId(),
                                match.getHomeFifa(),
                                match.getAwayFifa()
                        )
                );
            }
        }
        if (!loaded.isEmpty()) {
            Wc26ScheduleCatalog.installDbLookup(loaded);
        }
    }

    public Optional<Wc26ScheduleCatalog.GroupMatch> findById(int scheduleId) {
        return Wc26ScheduleCatalog.findById(scheduleId);
    }
}
