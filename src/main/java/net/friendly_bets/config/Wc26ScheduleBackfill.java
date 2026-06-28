package net.friendly_bets.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.gameresults.config.MatchResultSyncProperties;
import net.friendly_bets.wc26.Wc26ScheduleLinker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Links existing WC game_results to wc26_schedule after seed. */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class Wc26ScheduleBackfill implements ApplicationRunner {

    private final Wc26ScheduleLinker wc26ScheduleLinker;
    private final MatchResultSyncProperties matchResultSyncProperties;

    @Override
    public void run(ApplicationArguments args) {
        String season = matchResultSyncProperties.getDefaultSeason();
        wc26ScheduleLinker.backfillSeason(season);
        wc26ScheduleLinker.backfillSeason("2026");
        wc26ScheduleLinker.backfillKickoffSeason(season);
        wc26ScheduleLinker.backfillKickoffSeason("2026");
    }
}
