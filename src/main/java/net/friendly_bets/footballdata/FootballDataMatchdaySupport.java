package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.utils.SeasonCalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FootballDataMatchdaySupport {

    private static final Logger log = LoggerFactory.getLogger(FootballDataMatchdaySupport.class);

    private final FootballDataProperties properties;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;

    /**
     * Год football-data.org из активного сезона ({@code startDate} → год старта, напр. 2024/25 → 2024).
     */
    public String resolveFootballDataSeasonYear(Season season) {
        Integer year = SeasonCalendarUtils.resolveExternalSeasonYear(season.getStartDate());
        if (year != null) {
            return String.valueOf(year);
        }
        if (properties.getDefaultSeason() != null && !properties.getDefaultSeason().isBlank()) {
            return properties.getDefaultSeason().trim();
        }
        throw new IllegalStateException("Cannot resolve football-data season year for season " + season.getId());
    }

    public Optional<Integer> resolveSlotOrder(League league, String matchDay) {
        if (league.getTournamentFormatId() != null && !league.getTournamentFormatId().isBlank()) {
            try {
                TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
                Optional<Integer> fromFormat = tournamentFormatExpander.resolveOrder(format, matchDay);
                if (fromFormat.isPresent()) {
                    return fromFormat;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve slot for matchDay '{}' league {}: {}", matchDay, league.getId(), e.getMessage());
            }
        }
        try {
            return Optional.of(Integer.parseInt(matchDay));
        } catch (NumberFormatException e) {
            log.warn("Cannot parse matchDay '{}' for league {}", matchDay, league.getLeagueCode());
            return Optional.empty();
        }
    }

    public Optional<FootballDataMatchdayKey> buildMatchdayKey(League league, String matchDay, String footballDataSeason) {
        Optional<String> competitionCode = FootballDataCompetitionMapping.toCompetitionCode(league.getLeagueCode());
        if (competitionCode.isEmpty()) {
            return Optional.empty();
        }
        return resolveSlotOrder(league, matchDay)
                .map(order -> new FootballDataMatchdayKey(
                        competitionCode.get(),
                        order,
                        footballDataSeason,
                        league.getId()
                ));
    }
}
