package net.friendly_bets.services;

import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.repositories.PlayerStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles non-unique {@code player_stats} rows for the same season/league/user.
 * Hot path only picks a row (no deletes). Destructive cleanup is admin-script only.
 */
public final class PlayerStatsDedupSupport {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatsDedupSupport.class);

    private PlayerStatsDedupSupport() {
    }

    /**
     * Hot-path safe: returns one row to use. Does not delete.
     * When duplicates exist, logs a warning — cleanup via admin dedupe script + full recalc.
     */
    public static PlayerStats pickOne(List<PlayerStats> rows, String seasonId, String leagueId, String userId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (rows.size() == 1) {
            return rows.get(0);
        }
        PlayerStats keep = pickCanonical(rows);
        log.warn(
                "player_stats duplicates detected (no auto-delete) seasonId={} leagueId={} userId={} count={} using={}",
                seasonId,
                leagueId,
                userId,
                rows.size(),
                keep != null ? keep.getId() : null
        );
        return keep;
    }

    /**
     * Admin-only: keep canonical row, delete the rest. Returns number of deleted docs.
     */
    public static int deleteExtras(PlayerStatsRepository repository, List<PlayerStats> rows) {
        if (repository == null || rows == null || rows.size() <= 1) {
            return 0;
        }
        PlayerStats keep = pickCanonical(rows);
        if (keep == null || keep.getId() == null) {
            return 0;
        }
        List<String> removedIds = new ArrayList<>();
        for (PlayerStats row : rows) {
            if (row == null || row.getId() == null || row.getId().equals(keep.getId())) {
                continue;
            }
            repository.deleteById(row.getId());
            removedIds.add(row.getId());
        }
        if (!removedIds.isEmpty()) {
            log.warn(
                    "Admin dedupe player_stats kept={} removed={}",
                    keep.getId(),
                    removedIds
            );
        }
        return removedIds.size();
    }

    static PlayerStats pickCanonical(List<PlayerStats> rows) {
        return rows.stream()
                .filter(r -> r != null && r.getId() != null)
                .max(Comparator
                        .comparingInt((PlayerStats p) -> p.getBetCount() != null ? p.getBetCount() : 0)
                        .thenComparingInt(p -> p.getTotalBets() != null ? p.getTotalBets() : 0)
                        .thenComparing(PlayerStats::getId, Comparator.reverseOrder()))
                .orElse(null);
    }
}
