package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.aiscore.AiscoreTeamNamesService;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.marathonbet.MarathonbetTeamNamesService;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.twentyfourscore.TwentyFourScoreTeamNamesService;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalTeamNamesService {

    private final Soccer365TeamNamesService soccer365TeamNamesService;
    private final MarathonbetTeamNamesService marathonbetTeamNamesService;
    private final TwentyFourScoreTeamNamesService twentyFourScoreTeamNamesService;
    private final AiscoreTeamNamesService aiscoreTeamNamesService;

    public List<Soccer365TeamNameChipDto> fetchUnmappedTeamNames(String providerRaw, String leagueCode) {
        String provider = normalizeProvider(providerRaw);
        return switch (provider) {
            case TeamTitleUtils.SOCCER365_PROVIDER -> soccer365TeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case TeamTitleUtils.MARATHONBET_PROVIDER -> marathonbetTeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case TeamTitleUtils.TWENTYFOUR_SCORE_PROVIDER ->
                    twentyFourScoreTeamNamesService.fetchUnmappedTeamNames(leagueCode);
            case TeamTitleUtils.AISCORE_PROVIDER -> aiscoreTeamNamesService.fetchUnmappedTeamNames(leagueCode);
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
