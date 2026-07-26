package net.friendly_bets.aiscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.aiscore.config.AiscoreProperties;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiscoreTeamNamesService {

    private final AiscoreProperties properties;
    private final AiscoreHttpClient httpClient;
    private final AiscoreScheduleParser scheduleParser;
    private final TeamAliasResolver teamAliasResolver;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<Soccer365TeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        String path = requireTournamentPath(leagueCode);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String html = httpClient.fetchScheduleHtml(path);
        List<String> names = scheduleParser.parseAllTeamNames(html, path);
        if (names.isEmpty()) {
            throw new BadRequestException("aiscoreTeamNamesEmpty");
        }
        List<Soccer365TeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : names) {
            boolean mapped = teamAliasResolver
                    .resolveByProviderName(TeamTitleUtils.AISCORE_PROVIDER, name)
                    .isPresent();
            if (!mapped) {
                unmapped.add(Soccer365TeamNameChipDto.builder()
                        .externalName(name)
                        .provider(TeamTitleUtils.AISCORE_PROVIDER)
                        .alreadyMapped(false)
                        .build());
            }
        }
        return unmapped;
    }

    public static League.LeagueCode parseLeagueCode(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }

    String requireTournamentPath(League.LeagueCode leagueCode) {
        String path = properties.getTournamentPaths().get(leagueCode.name());
        if (path == null || path.isBlank()) {
            throw new BadRequestException("aiscoreTournamentNotConfigured");
        }
        return path.trim();
    }
}
