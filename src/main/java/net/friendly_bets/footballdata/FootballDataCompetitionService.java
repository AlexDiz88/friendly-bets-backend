package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.client.FootballDataClient;
import net.friendly_bets.footballdata.client.dto.FootballDataCompetitionResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FootballDataCompetitionService {

    private final FootballDataClient footballDataClient;

    public ExternalCompetitionInfoDto getCompetitionInfo(String competitionCode, String season) {
        if (!footballDataClient.isConfigured()) {
            return fallback(competitionCode, season);
        }
        try {
            FootballDataCompetitionResponse response = footballDataClient.fetchCompetition(competitionCode);
            if (response == null) {
                return fallback(competitionCode, season);
            }
            int matchdayCount = resolveMatchdayCount(response);
            int currentMatchday = resolveCurrentMatchday(response, season, matchdayCount);

            return ExternalCompetitionInfoDto.builder()
                    .competitionCode(competitionCode)
                    .season(season)
                    .currentMatchday(currentMatchday)
                    .matchdayCount(matchdayCount)
                    .build();
        } catch (BadRequestException e) {
            return fallback(competitionCode, season);
        }
    }

    private ExternalCompetitionInfoDto fallback(String competitionCode, String season) {
        int matchdayCount = FootballDataCompetitionDefaults.defaultMatchdayCount(competitionCode);
        return ExternalCompetitionInfoDto.builder()
                .competitionCode(competitionCode)
                .season(season)
                .currentMatchday(1)
                .matchdayCount(matchdayCount)
                .build();
    }

    private int resolveMatchdayCount(FootballDataCompetitionResponse response) {
        if (response.getSeasons() == null || response.getSeasons().isEmpty()) {
            return FootballDataCompetitionDefaults.defaultMatchdayCount(response.getCode());
        }

        return response.getSeasons().stream()
                .filter(this::isRegularSeason)
                .map(FootballDataCompetitionResponse.Season::getCurrentMatchday)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElseGet(() -> FootballDataCompetitionDefaults.defaultMatchdayCount(response.getCode()));
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

    private boolean isRegularSeason(FootballDataCompetitionResponse.Season season) {
        List<String> stages = season.getStages();
        return stages == null || stages.isEmpty() || stages.contains("REGULAR_SEASON");
    }
}
