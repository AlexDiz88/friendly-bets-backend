package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.client.FootballDataClient;
import net.friendly_bets.footballdata.client.dto.FootballDataCompetitionResponse;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.LeagueMatchdayService;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.services.TournamentFormatsService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FootballDataCompetitionService {

    private final FootballDataClient footballDataClient;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final LeagueMatchdayService leagueMatchdayService;

    public ExternalCompetitionInfoDto getCompetitionInfoForLeague(String leagueId, String season) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        Optional<String> competitionCode = FootballDataCompetitionMapping.toCompetitionCode(league.getLeagueCode());
        if (competitionCode.isEmpty()) {
            throw new BadRequestException("leagueNotMappedToExternalCompetition");
        }

        String code = competitionCode.get();
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return getCompetitionInfo(code, season).toBuilder().leagueId(leagueId).build();
        }

        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        List<ExternalMatchdaySlotDto> slots = tournamentFormatExpander.expand(format).stream()
                .map(TournamentFormatsService::toExternalSlot)
                .toList();

        int matchdayCount = slots.size();
        String effectiveSlotId = leagueMatchdayService.resolveEffectiveCurrentMatchDay(league);
        int currentMatchday = tournamentFormatExpander.resolveOrder(format, effectiveSlotId)
                .orElse(1);

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
        if (!footballDataClient.isConfigured()) {
            return fallback(competitionCode, season, null);
        }
        try {
            FootballDataCompetitionResponse response = footballDataClient.fetchCompetition(competitionCode);
            if (response == null) {
                return fallback(competitionCode, season, null);
            }
            int matchdayCount = FootballDataCompetitionDefaults.totalMatchdaySlots(competitionCode);
            int currentMatchday = resolveCurrentMatchday(response, season, matchdayCount);

            return ExternalCompetitionInfoDto.builder()
                    .competitionCode(competitionCode)
                    .season(season)
                    .currentMatchday(currentMatchday)
                    .matchdayCount(matchdayCount)
                    .matchdaySlots(FootballDataKnockoutMatchdays.buildMatchdaySlots(competitionCode))
                    .build();
        } catch (BadRequestException e) {
            return fallback(competitionCode, season, null);
        }
    }

    private ExternalCompetitionInfoDto fallback(String competitionCode, String season, String leagueId) {
        int matchdayCount = FootballDataCompetitionDefaults.totalMatchdaySlots(competitionCode);
        return ExternalCompetitionInfoDto.builder()
                .competitionCode(competitionCode)
                .season(season)
                .leagueId(leagueId)
                .currentMatchday(1)
                .matchdayCount(matchdayCount)
                .matchdaySlots(FootballDataKnockoutMatchdays.buildMatchdaySlots(competitionCode))
                .build();
    }

    private int resolveCurrentSlotOrder(
            String competitionCode,
            String season,
            TournamentFormat format,
            int slotCount
    ) {
        if (!footballDataClient.isConfigured()) {
            return 1;
        }
        try {
            FootballDataCompetitionResponse response = footballDataClient.fetchCompetition(competitionCode);
            if (response == null) {
                return 1;
            }
            int apiMatchday = resolveCurrentMatchday(response, season, FootballDataCompetitionDefaults.regularSeasonMatchdayCount(competitionCode));
            return tournamentFormatExpander.findBySlotId(format, String.valueOf(apiMatchday))
                    .map(ExpandedMatchdaySlot::getOrder)
                    .orElse(Math.min(apiMatchday, slotCount));
        } catch (BadRequestException e) {
            return 1;
        }
    }

    private int resolveCurrentMatchday(
            FootballDataCompetitionResponse response,
            String season,
            int matchdayCount
    ) {
        Optional<FootballDataCompetitionResponse.Season> matchedSeason = findSeason(response, season);

        Integer fromSeason = matchedSeason
                .map(FootballDataCompetitionResponse.Season::getCurrentMatchday)
                .orElse(null);

        if (fromSeason == null
                && response.getCurrentSeason() != null
                && seasonMatches(season, response.getCurrentSeason())) {
            fromSeason = response.getCurrentSeason().getCurrentMatchday();
        }

        int matchday = fromSeason != null ? fromSeason : 1;
        return Math.max(1, Math.min(matchday, matchdayCount));
    }

    private Optional<FootballDataCompetitionResponse.Season> findSeason(
            FootballDataCompetitionResponse response,
            String season
    ) {
        if (response.getSeasons() == null) {
            return Optional.empty();
        }

        return response.getSeasons().stream()
                .filter(s -> seasonMatches(season, s))
                .max(Comparator.comparing(
                        FootballDataCompetitionResponse.Season::getStartDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));
    }

    private boolean seasonMatches(String seasonParam, FootballDataCompetitionResponse.Season season) {
        if (season == null || season.getStartDate() == null || season.getEndDate() == null) {
            return false;
        }
        int year;
        try {
            year = Integer.parseInt(seasonParam);
        } catch (NumberFormatException e) {
            return false;
        }
        int startYear = season.getStartDate().getYear();
        int endYear = season.getEndDate().getYear();
        return year == endYear || year == startYear;
    }
}
