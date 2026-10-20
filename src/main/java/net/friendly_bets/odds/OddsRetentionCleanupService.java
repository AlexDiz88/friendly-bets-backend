package net.friendly_bets.odds;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Purges {@code odds} for schedules whose FULL_MATCH {@code finalized_at} is older than retention.
 */
@Service
@RequiredArgsConstructor
public class OddsRetentionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(OddsRetentionCleanupService.class);

    private final MatchScheduleRepository matchScheduleRepository;
    private final OddsService oddsService;

    /**
     * @return number of schedules for which odds delete was attempted
     */
    public int purgeExpired() {
        Instant now = Instant.now();
        Instant cutoff = oddsService.retentionCutoff(now);
        List<MatchSchedule> expired = matchScheduleRepository.findByFinalizedAtLessThanEqual(cutoff);
        int purged = 0;
        for (MatchSchedule match : expired) {
            if (match.getId() == null || match.getId().isBlank()) {
                continue;
            }
            oddsService.deleteByMatchScheduleId(match.getId());
            purged++;
        }
        if (purged > 0) {
            log.info("odds retention cleanup: purged {} schedule(s) with finalized_at <= {}", purged, cutoff);
        }
        return purged;
    }
}
