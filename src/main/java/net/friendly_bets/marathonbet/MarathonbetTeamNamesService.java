package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarathonbetTeamNamesService {

    private final MarathonbetProperties properties;
    private final MarathonbetTournamentClient tournamentClient;
    private final TeamAliasResolver teamAliasResolver;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<Soccer365TeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        Long tournamentId = properties.tournamentTreeIdForLeague(leagueCode.name());
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }

        MarathonbetHttpFetchResult tournamentResult = tournamentClient.fetchTournament(tournamentId);
        if (!tournamentResult.isSuccess() || tournamentResult.getBody() == null) {
            throw new BadRequestException("marathonbetFetchFailed");
        }

        List<MarathonbetPrematchEvent> events =
                MarathonbetTournamentParser.parsePrematchEvents(tournamentResult.getBody());
        Set<String> uniqueNames = new LinkedHashSet<>();
        for (MarathonbetPrematchEvent event : events) {
            addName(uniqueNames, event.getHomeTeam());
            addName(uniqueNames, event.getAwayTeam());
        }
        if (uniqueNames.isEmpty()) {
            throw new BadRequestException("marathonbetTeamNamesEmpty");
        }

        List<Soccer365TeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : uniqueNames) {
            if (teamAliasResolver.resolveMarathonbetByName(name).isEmpty()) {
                unmapped.add(Soccer365TeamNameChipDto.builder()
                        .externalName(name)
                        .provider(TeamTitleUtils.MARATHONBET_PROVIDER)
                        .alreadyMapped(false)
                        .build());
            }
        }
        return unmapped;
    }

    private static void addName(Set<String> names, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        names.add(raw.trim());
    }
}
