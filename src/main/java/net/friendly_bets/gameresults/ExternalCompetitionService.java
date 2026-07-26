package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.services.TournamentFormatsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalCompetitionService {

    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final GameResultsCurrentSlotResolver gameResultsCurrentSlotResolver;

    public ExternalCompetitionInfoDto getCompetitionInfoForLeague(String leagueId, String season) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            throw new BadRequestException("leagueHasNoTournamentFormat");
        }

        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        List<ExternalMatchdaySlotDto> slots = tournamentFormatExpander.expand(format).stream()
                .map(TournamentFormatsService::toExternalSlot)
                .toList();

        int matchdayCount = slots.size();
        int currentMatchday = gameResultsCurrentSlotResolver.resolveCurrentSlotOrder(league, format, season);

        return ExternalCompetitionInfoDto.builder()
                .competitionCode(league.getLeagueCode().name())
                .season(season)
                .leagueId(leagueId)
                .currentMatchday(currentMatchday)
                .matchdayCount(matchdayCount)
                .matchdaySlots(slots)
                .build();
    }
}
