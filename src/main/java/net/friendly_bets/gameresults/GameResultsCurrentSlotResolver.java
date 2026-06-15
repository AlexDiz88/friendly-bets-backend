package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Текущий слот для страницы результатов: первый тур/слот, где ещё не все матчи завершены.
 */
@Component
@RequiredArgsConstructor
public class GameResultsCurrentSlotResolver {

    private final TournamentFormatExpander tournamentFormatExpander;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final TeamsRepository teamsRepository;

    public int resolveCurrentSlotOrder(League league, TournamentFormat format, String season) {
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);
        if (slots.isEmpty()) {
            return 1;
        }

        String leagueCode = league.getLeagueCode().name();
        String leagueId = league.getId();
        int lastOrder = slots.get(slots.size() - 1).getOrder();

        for (ExpandedMatchdaySlot slot : slots) {
            if (!isSlotComplete(leagueCode, leagueId, slot, season)) {
                return slot.getOrder();
            }
        }
        return lastOrder;
    }

    private boolean isSlotComplete(
            String leagueCode,
            String leagueId,
            ExpandedMatchdaySlot slot,
            String season
    ) {
        int slotOrder = slot.getOrder();
        String slotId = slot.getId();

        Optional<GameResultsSync> sync = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, slotOrder, season);
        List<GameResultRecord> records = loadSlotRecords(leagueCode, leagueId, slotId, slotOrder, season);

        if (WcBerlinSlotMatchFilter.isBerlinGroupSlot(slotId)) {
            return isBerlinSlotComplete(slotId, records);
        }

        if (sync.isPresent()) {
            return sync.get().getSyncStatus() == GameResultsSyncStatus.COMPLETED;
        }

        if (records.isEmpty()) {
            return false;
        }
        return records.stream().allMatch(GameResultRecord::isFinalized);
    }

    private boolean isBerlinSlotComplete(String slotId, List<GameResultRecord> records) {
        if (records.isEmpty()) {
            return false;
        }
        int expected = WcBerlinSlotMatchFilter.expectedMatchCount(slotId);
        long finalizedCount = records.stream().filter(GameResultRecord::isFinalized).count();
        return expected > 0
                && finalizedCount >= expected
                && records.stream().allMatch(GameResultRecord::isFinalized);
    }

    private List<GameResultRecord> loadSlotRecords(
            String leagueCode,
            String leagueId,
            String slotId,
            int slotOrder,
            String season
    ) {
        List<GameResultRecord> records = gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, slotOrder, season);
        if (!WcBerlinSlotMatchFilter.isBerlinGroupSlot(slotId)) {
            return records;
        }
        return WcBerlinSlotMatchFilter.filterGameResultRecords(
                slotId,
                records,
                teamId -> {
                    if (teamId == null || teamId.isBlank()) {
                        return Optional.empty();
                    }
                    return teamsRepository.findById(teamId);
                }
        );
    }
}
