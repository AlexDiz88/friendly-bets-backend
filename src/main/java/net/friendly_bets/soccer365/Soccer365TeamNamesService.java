package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Soccer365TeamNamesService {

    private final Soccer365Properties properties;
    private final Soccer365HttpClient httpClient;
    private final Soccer365ScheduleParser scheduleParser;
    private final TeamAliasResolver teamAliasResolver;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<Soccer365TeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        int competitionId = requireCompetitionId(leagueCode);
        // Ensure a running season exists (admin context); names themselves are site-global for current season page.
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String html = httpClient.fetchScheduleHtml(competitionId);
        List<String> names = scheduleParser.parseClubFilterNames(html);
        List<Soccer365TeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : names) {
            boolean mapped = teamAliasResolver.resolveSoccer365ByName(name).isPresent();
            if (!mapped) {
                unmapped.add(Soccer365TeamNameChipDto.builder()
                        .externalName(name)
                        .provider(TeamTitleUtils.SOCCER365_PROVIDER)
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

    int requireCompetitionId(League.LeagueCode leagueCode) {
        Integer id = properties.getCompetitionIds().get(leagueCode.name());
        if (id == null || id <= 0) {
            throw new BadRequestException("soccer365CompetitionNotConfigured");
        }
        return id;
    }
}
