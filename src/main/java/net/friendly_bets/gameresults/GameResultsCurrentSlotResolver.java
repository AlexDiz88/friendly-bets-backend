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
import net.friendly_bets.services.TournamentFormatExpander;
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

    public int resolveCurrentSlotOrder(League league, TournamentFormat format, String season) {
        List<ExpandedMatchdaySlot> slots = tournamentFormatExpander.expand(format);
        if (slots.isEmpty()) {
            return 1;
        }

        String leagueCode = league.getLeagueCode().name();
        int lastOrder = slots.get(slots.size() - 1).getOrder();

        for (ExpandedMatchdaySlot slot : slots) {
            if (!isSlotComplete(leagueCode, slot, season)) {
                return slot.getOrder();
            }
        }
        return lastOrder;
    }

    private boolean isSlotComplete(
            String leagueCode,
            ExpandedMatchdaySlot slot,
            String season
    ) {
        int slotOrder = slot.getOrder();

        Optional<GameResultsSync> sync = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, slotOrder, season);
        List<GameResultRecord> records = gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, slotOrder, season);

        if (sync.isPresent()) {
            return sync.get().getSyncStatus() == GameResultsSyncStatus.COMPLETED;
        }

        if (records.isEmpty()) {
            return false;
        }
        return records.stream().allMatch(GameResultRecord::isFinalized);
    }
}
