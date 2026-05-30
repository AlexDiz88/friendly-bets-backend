package net.friendly_bets.externaldata.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.externaldata.ExternalMatchDataProvider;
import net.friendly_bets.externaldata.ExternalMatchFetchRequest;
import net.friendly_bets.externaldata.ExternalSlotQuery;
import net.friendly_bets.footballdata.FootballDataLegFilter;
import net.friendly_bets.footballdata.client.FootballDataClient;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchdayResponse;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FootballDataExternalMatchDataProvider implements ExternalMatchDataProvider {

    private final FootballDataClient footballDataClient;
    private final FootballDataSlotQueryMapper slotQueryMapper;

    @Override
    public String providerId() {
        return FootballDataSlotQueryMapper.PROVIDER_ID;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return footballDataClient.isConfigured();
    }

    @Override
    public List<FootballDataMatchDto> fetchMatches(ExternalMatchFetchRequest request) {
        if (!isAvailable()) {
            throw new BadRequestException("footballDataApiKeyNotConfigured");
        }
        ExpandedMatchdaySlot slot = request.slot();
        if (slot == null) {
            throw new BadRequestException("invalidSlotOrder");
        }
        ExternalSlotQuery query = slotQueryMapper.map(slot, request.competitionCode())
                .orElseThrow(() -> new BadRequestException("invalidSlotOrder"));

        FootballDataMatchdayResponse response = switch (query.queryType()) {
            case MATCHDAY -> footballDataClient.fetchMatchday(
                    request.competitionCode(), query.matchday(), request.season());
            case STAGE -> footballDataClient.fetchMatchesByStage(
                    request.competitionCode(), query.stage(), request.season());
            case STAGE_LEG -> footballDataClient.fetchMatchesByStage(
                    request.competitionCode(), query.stage(), request.season());
        };

        if (response == null || response.getMatches() == null) {
            return List.of();
        }
        List<FootballDataMatchDto> matches;
        if (query.queryType() == ExternalSlotQuery.QueryType.STAGE_LEG && query.leg() != null) {
            matches = FootballDataLegFilter.filterByLeg(response.getMatches(), query.leg());
        } else {
            matches = response.getMatches();
        }
        if (slot.getKind() == ExpandedMatchdaySlot.Kind.GROUP
                && WcBerlinSlotMatchFilter.isBerlinGroupSlot(slot.getId())) {
            return WcBerlinSlotMatchFilter.filterFootballDataMatches(slot.getId(), matches);
        }
        return matches;
    }
}
