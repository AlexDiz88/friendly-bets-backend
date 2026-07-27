package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.config.Soccer365Properties;
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

    public List<ExternalTeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        int competitionId = requireCompetitionId(leagueCode);
        // Ensure a running season exists (admin context); names themselves are site-global for current season page.
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String html = httpClient.fetchScheduleHtml(competitionId);
        List<String> names = scheduleParser.parseTeamNamesFromMatchday(html, competitionId, 1);
        if (names.isEmpty()) {
            throw new BadRequestException("soccer365MatchdayOneTeamNamesEmpty");
        }
        List<ExternalTeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : names) {
            boolean mapped = teamAliasResolver.resolveByProviderName(ExternalProviderIds.SOCCER365, name).isPresent();
            if (!mapped) {
                unmapped.add(ExternalTeamNameChipDto.builder()
                        .externalName(name)
                        .provider(ExternalProviderIds.SOCCER365)
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
