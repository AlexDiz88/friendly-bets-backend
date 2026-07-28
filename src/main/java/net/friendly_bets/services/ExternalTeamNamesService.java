package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.football24.Football24TeamNamesService;
import net.friendly_bets.marathonbet.MarathonbetTeamNamesService;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.sportsru.SportsRuTeamNamesService;
import net.friendly_bets.twentyfourscore.TwentyFourScoreTeamNamesService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalTeamNamesService {

    private final Soccer365TeamNamesService soccer365TeamNamesService;
    private final MarathonbetTeamNamesService marathonbetTeamNamesService;
    private final TwentyFourScoreTeamNamesService twentyFourScoreTeamNamesService;
    private final SportsRuTeamNamesService sportsRuTeamNamesService;
    private final Football24TeamNamesService football24TeamNamesService;

    public List<ExternalTeamNameChipDto> fetchUnmappedTeamNames(String providerRaw, String leagueCode) {
        String provider = normalizeProvider(providerRaw);
        return switch (provider) {
            case ExternalProviderIds.SOCCER365 -> soccer365TeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case ExternalProviderIds.MARATHONBET -> marathonbetTeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case ExternalProviderIds.TWENTYFOUR_SCORE ->
                    twentyFourScoreTeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case ExternalProviderIds.SPORTS_RU -> sportsRuTeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case ExternalProviderIds.FOOTBALL24 -> football24TeamNamesService.fetchUnmappedTeamNames(leagueCode);
            default -> throw new BadRequestException("externalTeamNamesProviderUnsupported");
        };
    }

    private static String normalizeProvider(String providerRaw) {
        if (providerRaw == null || providerRaw.isBlank()) {
            throw new BadRequestException("externalTeamNamesProviderRequired");
        }
        return providerRaw.trim();
    }
}
