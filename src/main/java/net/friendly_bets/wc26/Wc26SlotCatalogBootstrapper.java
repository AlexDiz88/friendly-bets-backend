package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Creates {@code game_results} for WC betting slots when FIFA schedule already has both teams
 * but external providers (4score / 24score) have not listed the match yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Wc26SlotCatalogBootstrapper {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final TeamAliasResolver teamAliasResolver;
    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;
    private final Wc26ScheduleLinker wc26ScheduleLinker;

    public int bootstrapMissingKnownPairs(
            String slotId,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records
    ) {
        if (!"WC".equals(leagueCode) || !WcBerlinSlotMatchFilter.isWcBettingSlot(slotId)) {
            return 0;
        }
        int created = 0;
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            if (scheduleSlotFilled(scheduleId, records)) {
                continue;
            }
            Optional<Wc26ScheduleCatalog.GroupMatch> catalog = Wc26ScheduleCatalog.findById(scheduleId);
            if (catalog.isEmpty()) {
                continue;
            }
            Wc26ScheduleCatalog.GroupMatch pair = catalog.get();
            if (pair.homeFifa() == null || pair.awayFifa() == null) {
                continue;
            }
            Optional<Team> home = teamAliasResolver.resolveWc26Code(pair.homeFifa());
            Optional<Team> away = teamAliasResolver.resolveWc26Code(pair.awayFifa());
            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }
            Optional<GameResultRecord> existingPair = records.stream()
                    .filter(r -> home.get().getId().equals(r.getHomeTeamId())
                            && away.get().getId().equals(r.getAwayTeamId()))
                    .findFirst();
            if (existingPair.isPresent()) {
                GameResultRecord record = existingPair.get();
                if (!Integer.valueOf(scheduleId).equals(record.getWc26ScheduleId())) {
                    record.setWc26ScheduleId(scheduleId);
                    wc26ScheduleLinker.backfillKickoffFromSchedule(record);
                    gameResultRecordRepository.save(record);
                }
                continue;
            }
            GameResultRecord saved = createScheduledRecord(
                    scheduleId,
                    pair,
                    home.get(),
                    away.get(),
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    fetchedAt
            );
            records.add(saved);
            created++;
            log.info(
                    "WC catalog bootstrap: schedule {} — {} vs {} (slot {})",
                    scheduleId,
                    pair.homeFifa(),
                    pair.awayFifa(),
                    slotId
            );
        }
        return created;
    }

    private static boolean scheduleSlotFilled(int scheduleId, List<GameResultRecord> records) {
        return records.stream().anyMatch(r -> Integer.valueOf(scheduleId).equals(r.getWc26ScheduleId()));
    }

    private GameResultRecord createScheduledRecord(
            int scheduleId,
            Wc26ScheduleCatalog.GroupMatch pair,
            Team home,
            Team away,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            LocalDateTime fetchedAt
    ) {
        LocalDateTime kickoff = wc26ScheduleKickoffResolver.kickoffUtc(scheduleId).orElse(null);
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .externalMatchId(-scheduleId)
                .externalSeason(season)
                .status("SCHEDULED")
                .utcDate(kickoff)
                .home(sideSnapshot(home, pair.homeFifa()))
                .away(sideSnapshot(away, pair.awayFifa()))
                .fetchedAt(fetchedAt)
                .build();
        GameResultRecord record = GameResultRecord.builder()
                .leagueCode(leagueCode)
                .matchday(matchday)
                .season(season)
                .leagueId(leagueId)
                .homeTeamId(home.getId())
                .awayTeamId(away.getId())
                .status("SCHEDULED")
                .utcDate(kickoff)
                .fetchedAt(fetchedAt)
                .wc26ScheduleId(scheduleId)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        source
                ))
                .build();
        GameResultRecord saved = gameResultRecordRepository.save(record);
        wc26ScheduleLinker.backfillKickoffFromSchedule(saved);
        return gameResultRecordRepository.save(saved);
    }

    private static GameResultSideSnapshot sideSnapshot(Team team, String fifaCode) {
        String name = team.getTitle() != null && !team.getTitle().isBlank() ? team.getTitle() : fifaCode;
        return GameResultSideSnapshot.builder().externalName(name).build();
    }
}
