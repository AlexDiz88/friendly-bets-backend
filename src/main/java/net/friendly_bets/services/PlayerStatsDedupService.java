package net.friendly_bets.services;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.PlayerStatsDedupResultDto;
import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.repositories.PlayerStatsRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only cleanup: remove duplicate player_stats, then ensure unique index.
 * Run full player-stats recalculation after this when duplicates were removed.
 */
@Service
@RequiredArgsConstructor
public class PlayerStatsDedupService {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatsDedupService.class);
    public static final String INDEX_NAME = "season_league_user_unique";

    private final PlayerStatsRepository playerStatsRepository;
    private final MongoTemplate mongoTemplate;

    public PlayerStatsDedupResultDto dedupeAllAndEnsureUniqueIndex() {
        List<PlayerStats> all = playerStatsRepository.findAll();
        Map<String, List<PlayerStats>> byKey = new LinkedHashMap<>();
        for (PlayerStats row : all) {
            if (row == null || row.getSeasonId() == null || row.getLeagueId() == null
                    || row.getUser() == null || row.getUser().getId() == null) {
                continue;
            }
            String key = row.getSeasonId() + "|" + row.getLeagueId() + "|" + row.getUser().getId();
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        int groupsWithDuplicates = 0;
        int documentsRemoved = 0;
        for (List<PlayerStats> group : byKey.values()) {
            if (group.size() <= 1) {
                continue;
            }
            groupsWithDuplicates++;
            documentsRemoved += PlayerStatsDedupSupport.deleteExtras(playerStatsRepository, group);
        }

        boolean indexOk = ensureUniqueIndex();
        String message = groupsWithDuplicates == 0
                ? "playerStatsDedupNoDuplicates"
                : "playerStatsDedupDone";
        log.info(
                "player_stats dedupe: groups={}, dupGroups={}, removed={}, index={}",
                byKey.size(),
                groupsWithDuplicates,
                documentsRemoved,
                indexOk
        );
        return PlayerStatsDedupResultDto.builder()
                .groupsScanned(byKey.size())
                .groupsWithDuplicates(groupsWithDuplicates)
                .documentsRemoved(documentsRemoved)
                .uniqueIndexEnsured(indexOk)
                .message(message)
                .build();
    }

    private boolean ensureUniqueIndex() {
        try {
            mongoTemplate.getCollection("player_stats").createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("seasonId"),
                            Indexes.ascending("leagueId"),
                            Indexes.ascending("user.$id")
                    ),
                    new IndexOptions().name(INDEX_NAME).unique(true)
            );
            return true;
        } catch (RuntimeException e) {
            for (Document existing : mongoTemplate.getCollection("player_stats").listIndexes()) {
                if (INDEX_NAME.equals(existing.getString("name"))) {
                    Boolean unique = existing.getBoolean("unique");
                    return Boolean.TRUE.equals(unique);
                }
            }
            log.warn("Failed to ensure player_stats unique index: {}", e.getMessage());
            return false;
        }
    }
}
