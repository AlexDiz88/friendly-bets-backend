package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameResultQueryService {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final TeamsRepository teamsRepository;

    public List<GameResultRecord> getMatches(
            String pathLeagueOrCompetitionCode,
            int matchday,
            String season,
            String leagueId
    ) {
        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);
        return getMatchesByLeagueCode(leagueCode, matchday, season, leagueId);
    }

    public List<GameResultRecord> getMatchesByLeagueCode(
            String leagueCode,
            int matchday,
            String season,
            String leagueId
    ) {
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                leagueCode, matchday, season);
        return applyBerlinFilterIfNeeded(matches, leagueId, matchday);
    }

    /** Все записи тура без Berlin-фильтра — для sync (иначе playoff без wc26ScheduleId «невидим»). */
    public List<GameResultRecord> listMatchdayRecordsForSync(
            String leagueCode,
            int matchday,
            String season
    ) {
        return gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                leagueCode, matchday, season);
    }

    private List<GameResultRecord> applyBerlinFilterIfNeeded(
            List<GameResultRecord> matches,
            String leagueId,
            int slotOrder
    ) {
        return resolveSlotId(leagueId, slotOrder)
                .filter(WcBerlinSlotMatchFilter::isWcBettingSlot)
                .map(slotId -> WcBerlinSlotMatchFilter.filterGameResultRecords(
                        slotId,
                        matches,
                        teamId -> {
                            if (teamId == null || teamId.isBlank()) {
                                return Optional.empty();
                            }
                            return teamsRepository.findById(teamId);
                        })
                        .stream()
                        .filter(record -> !WcBerlinSlotMatchFilter.isPlayoffSlot(slotId)
                                || !WcBerlinSlotMatchFilter.recordHasPlaceholderSides(record))
                        .toList())
                .orElse(matches);
    }

    private Optional<String> resolveSlotId(String leagueId, int slotOrder) {
        if (leagueId == null || leagueId.isBlank()) {
            return Optional.empty();
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return Optional.empty();
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        return tournamentFormatExpander.findByOrder(format, slotOrder).map(ExpandedMatchdaySlot::getId);
    }
}
