package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.OddsProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MelbetOddsProvider implements OddsProvider {

    private final MelbetSyncService melbetSyncService;

    @Override
    public String providerId() {
        return ExternalProviderIds.MELBET;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.ODDS);
    }

    @Override
    public OddsSyncResult syncLeagueSlot(
            Season season,
            League league,
            String matchdaySlotId,
            List<String> matchScheduleIds
    ) {
        String code = league != null && league.getLeagueCode() != null
                ? league.getLeagueCode().name()
                : null;
        MelbetSyncResult result = melbetSyncService.syncLeague(code, false);
        int matchday = 0;
        if (result.getSlotOrders() != null && !result.getSlotOrders().isEmpty()) {
            matchday = result.getSlotOrders().get(0);
        }
        return new OddsSyncResult(
                result.getLeagueCode() != null ? result.getLeagueCode() : code,
                matchday,
                result.getMatchesMatched(),
                result.getMergedSaved(),
                result.getSkippedFar() + result.getMappingFailures(),
                result.getErrorSummary()
        );
    }
}
