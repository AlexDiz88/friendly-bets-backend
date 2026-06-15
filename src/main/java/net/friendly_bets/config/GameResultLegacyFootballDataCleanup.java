package net.friendly_bets.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.fourscore.FourScorePlayoffPlaceholderNames;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Removes legacy {@code football-data} snapshots left after provider removal so 4score can backfill.
 */
@Component
@Order(101)
@RequiredArgsConstructor
@Slf4j
public class GameResultLegacyFootballDataCleanup implements ApplicationRunner {

    private static final String LEGACY_PROVIDER = "football-data";
    private static final String LEGACY_SOURCE_KEY = "football_data";

    private final GameResultRecordRepository gameResultRecordRepository;
    private final ApiSyncIssueRepository apiSyncIssueRepository;

    @Override
    public void run(ApplicationArguments args) {
        int cleanedRecords = cleanupGameResults();
        int purgedIssues = purgePlayoffPlaceholderIssues();
        if (cleanedRecords > 0 || purgedIssues > 0) {
            log.info(
                    "Legacy football-data cleanup: {} game_results updated, {} placeholder mapping issues removed",
                    cleanedRecords,
                    purgedIssues
            );
        }
    }

    private int cleanupGameResults() {
        int updated = 0;
        for (GameResultRecord record : gameResultRecordRepository.findAll()) {
            if (!hasLegacyFootballData(record)) {
                continue;
            }
            if (record.getSources() != null && record.getSources().containsKey(LEGACY_SOURCE_KEY)) {
                Map<String, GameResultSourceSnapshot> sources =
                        new HashMap<>(record.getSources());
                sources.remove(LEGACY_SOURCE_KEY);
                record.setSources(sources);
            }
            if (LEGACY_PROVIDER.equals(record.getProvider()) && !hasActiveScoreProvider(record)) {
                record.setProvider(null);
            }
            gameResultRecordRepository.save(record);
            updated++;
        }
        return updated;
    }

    private int purgePlayoffPlaceholderIssues() {
        int purged = 0;
        List<ApiSyncIssue> issues = apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc();
        for (ApiSyncIssue issue : issues) {
            if (!ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name().equals(issue.getIssueType())) {
                continue;
            }
            if (!MatchDataProviders.FOURSCORE.equals(issue.getProvider())) {
                continue;
            }
            if (FourScorePlayoffPlaceholderNames.isPlaceholder(issue.getHomeTeamName())
                    || FourScorePlayoffPlaceholderNames.isPlaceholder(issue.getAwayTeamName())) {
                apiSyncIssueRepository.delete(issue);
                purged++;
            }
        }
        return purged;
    }

    private static boolean hasLegacyFootballData(GameResultRecord record) {
        if (record == null) {
            return false;
        }
        if (LEGACY_PROVIDER.equals(record.getProvider())) {
            return true;
        }
        Map<String, ?> sources = record.getSources();
        return sources != null && sources.containsKey(LEGACY_SOURCE_KEY);
    }

    private static boolean hasActiveScoreProvider(GameResultRecord record) {
        return record.fourScoreSource() != null || record.twentyFourScoreSource() != null;
    }
}
