package net.friendly_bets.sportsru;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.sportsru.config.SportsRuProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsRuTeamNamesService {

    private final SportsRuProperties properties;
    private final SportsRuHttpClient httpClient;
    private final SportsRuScheduleParser scheduleParser;
    private final TeamAliasResolver teamAliasResolver;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<ExternalTeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        requireScheduleSyncSupported(leagueCode);
        String calendarPath = requireCalendarPath(leagueCode);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String html = httpClient.fetchCalendarHtml(calendarPath);
        List<String> names = scheduleParser.parseTeamNamesFromMatchday(html, 1);
        if (names.isEmpty()) {
            throw new BadRequestException("sportsRuMatchdayOneTeamNamesEmpty");
        }
        List<ExternalTeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : names) {
            boolean mapped = teamAliasResolver.resolveByProviderName(ExternalProviderIds.SPORTS_RU, name).isPresent();
            if (!mapped) {
                unmapped.add(ExternalTeamNameChipDto.builder()
                        .externalName(name)
                        .provider(ExternalProviderIds.SPORTS_RU)
                        .alreadyMapped(false)
                        .build());
            }
        }
        return unmapped;
    }

    public boolean isScheduleSyncSupported(League.LeagueCode leagueCode) {
        if (leagueCode == null) {
            return false;
        }
        Set<String> enabled = properties.getScheduleSyncLeagueCodes();
        return enabled != null && enabled.contains(leagueCode.name());
    }

    public void requireScheduleSyncSupported(League.LeagueCode leagueCode) {
        if (!isScheduleSyncSupported(leagueCode)) {
            throw new BadRequestException("sportsRuLeagueScheduleNotSupported");
        }
    }

    public String requireCalendarPath(League.LeagueCode leagueCode) {
        String path = properties.getCalendarPaths() != null
                ? properties.getCalendarPaths().get(leagueCode.name())
                : null;
        if (path == null || path.isBlank()) {
            throw new BadRequestException("sportsRuCalendarPathNotConfigured");
        }
        return path.trim();
    }
}
