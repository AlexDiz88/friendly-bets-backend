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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalCompetitionService {

    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final GameResultsCurrentSlotResolver gameResultsCurrentSlotResolver;

    public ExternalCompetitionInfoDto getCompetitionInfoForLeague(String leagueId, String season) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        Optional<String> competitionCode = LeagueCompetitionMapping.toCompetitionCode(league.getLeagueCode());
        if (competitionCode.isEmpty()) {
            throw new BadRequestException("leagueNotMappedToExternalCompetition");
        }

        String code = competitionCode.get();
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return fallback(code, season, leagueId);
        }

        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        List<ExternalMatchdaySlotDto> slots = tournamentFormatExpander.expand(format).stream()
                .map(TournamentFormatsService::toExternalSlot)
                .toList();

        int matchdayCount = slots.size();
        int currentMatchday = gameResultsCurrentSlotResolver.resolveCurrentSlotOrder(league, format, season);

        return ExternalCompetitionInfoDto.builder()
                .competitionCode(code)
                .season(season)
                .leagueId(leagueId)
                .currentMatchday(currentMatchday)
                .matchdayCount(matchdayCount)
                .matchdaySlots(slots)
                .build();
    }

    public ExternalCompetitionInfoDto getCompetitionInfo(String competitionCode, String season) {
        return fallback(competitionCode, season, null);
    }

    private ExternalCompetitionInfoDto fallback(String competitionCode, String season, String leagueId) {
        int matchdayCount = LeagueSlotDefaults.totalMatchdaySlots(competitionCode);
        return ExternalCompetitionInfoDto.builder()
                .competitionCode(competitionCode)
                .season(season)
                .leagueId(leagueId)
                .currentMatchday(1)
                .matchdayCount(matchdayCount)
                .matchdaySlots(LeagueSlotDefaults.buildMatchdaySlots(competitionCode))
                .build();
    }
}
