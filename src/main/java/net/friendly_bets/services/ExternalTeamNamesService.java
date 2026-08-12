package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.championat.ChampionatTeamNamesService;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.dto.ExternalTeamNamesLoadResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.football24.Football24TeamNamesService;
import net.friendly_bets.marathonbet.MarathonbetTeamNamesService;
import net.friendly_bets.melbet.MelbetTeamNamesService;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.ruscore.RuscoreTeamNamesService;
import net.friendly_bets.flashscore.FlashscoreTeamNamesService;
import net.friendly_bets.liveresult.LiveresultTeamNamesService;
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
    private final MelbetTeamNamesService melbetTeamNamesService;
    private final TwentyFourScoreTeamNamesService twentyFourScoreTeamNamesService;
    private final SportsRuTeamNamesService sportsRuTeamNamesService;
    private final Football24TeamNamesService football24TeamNamesService;
    private final ChampionatTeamNamesService championatTeamNamesService;
    private final RuscoreTeamNamesService ruscoreTeamNamesService;
    private final FlashscoreTeamNamesService flashscoreTeamNamesService;
    private final LiveresultTeamNamesService liveresultTeamNamesService;
    private final ExternalTeamAliasAutoBindService autoBindService;

    public ExternalTeamNamesLoadResultDto fetchAndAutoBindTeamNames(String providerRaw, String leagueCode) {
        return fetchAndAutoBindTeamNames(providerRaw, leagueCode, false);
    }

    public ExternalTeamNamesLoadResultDto fetchAndAutoBindTeamNames(
            String providerRaw,
            String leagueCode,
            boolean forceOverwrite
    ) {
        String provider = normalizeProvider(providerRaw);
        List<String> names = switch (provider) {
            case ExternalProviderIds.SOCCER365 -> soccer365TeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.MARATHONBET -> marathonbetTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.MELBET -> melbetTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.TWENTYFOUR_SCORE ->
                    twentyFourScoreTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.SPORTS_RU -> sportsRuTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.FOOTBALL24 -> football24TeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.CHAMPIONAT -> championatTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.RUSCORE -> ruscoreTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.FLASHSCORE -> flashscoreTeamNamesService.fetchTeamNames(leagueCode);
            case ExternalProviderIds.LIVERESULT -> liveresultTeamNamesService.fetchTeamNames(leagueCode);
            default -> throw new BadRequestException("externalTeamNamesProviderUnsupported");
        };
        return autoBindService.bindAndCollectUnmapped(provider, leagueCode, names, forceOverwrite);
    }

    /** @deprecated use {@link #fetchAndAutoBindTeamNames}; kept for legacy soccer365 admin route. */
    @Deprecated
    public List<ExternalTeamNameChipDto> fetchUnmappedTeamNames(String providerRaw, String leagueCode) {
        return fetchAndAutoBindTeamNames(providerRaw, leagueCode).getUnmapped();
    }

    private static String normalizeProvider(String providerRaw) {
        if (providerRaw == null || providerRaw.isBlank()) {
            throw new BadRequestException("externalTeamNamesProviderRequired");
        }
        return providerRaw.trim();
    }
}
