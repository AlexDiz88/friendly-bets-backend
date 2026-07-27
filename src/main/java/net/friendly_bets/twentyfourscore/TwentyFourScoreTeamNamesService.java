package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TwentyFourScoreTeamNamesService {

    private final TwentyFourScoreProperties properties;
    private final TwentyFourScoreHttpClient httpClient;
    private final TwentyFourScoreStandingsParser standingsParser;
    private final TeamAliasResolver teamAliasResolver;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySlotSupport;

    public List<ExternalTeamNameChipDto> fetchUnmappedTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String pathTemplate = requireStandingsPathTemplate(leagueCode);
        String seasonSegment = resolveSeasonSegment(season);
        String standingsPath = pathTemplate.replace("{season}", seasonSegment);

        String dataHtml = httpClient.fetchStandingsDataHtml(standingsPath);
        List<String> names = standingsParser.parseTeamNames(dataHtml);
        if (names.isEmpty()) {
            throw new BadRequestException("twentyFourScoreTeamNamesEmpty");
        }

        List<ExternalTeamNameChipDto> unmapped = new ArrayList<>();
        for (String name : names) {
            if (teamAliasResolver.resolveByProviderName(ExternalProviderIds.TWENTYFOUR_SCORE, name).isEmpty()) {
                unmapped.add(ExternalTeamNameChipDto.builder()
                        .externalName(name)
                        .provider(ExternalProviderIds.TWENTYFOUR_SCORE)
                        .alreadyMapped(false)
                        .build());
            }
        }
        return unmapped;
    }

    private String requireStandingsPathTemplate(League.LeagueCode leagueCode) {
        Map<String, String> paths = properties.getStandingsPaths();
        if (paths == null || paths.isEmpty()) {
            throw new BadRequestException("twentyFourScoreStandingsNotConfigured");
        }
        String path = paths.get(leagueCode.name());
        if (path == null || path.isBlank()) {
            throw new BadRequestException("twentyFourScoreStandingsNotConfigured");
        }
        return path.trim();
    }

    private String resolveSeasonSegment(Season season) {
        String yearRaw = matchdaySlotSupport.resolveExternalSeasonYear(season);
        try {
            int year = Integer.parseInt(yearRaw.trim());
            return year + "-" + (year + 1);
        } catch (NumberFormatException e) {
            throw new BadRequestException("seasonDatesRequired");
        }
    }
}
