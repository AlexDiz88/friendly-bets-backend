package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.config.MatchResultSyncProperties;
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
public class MatchdaySlotSupport {

    private static final Logger log = LoggerFactory.getLogger(MatchdaySlotSupport.class);

    private final MatchResultSyncProperties properties;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;

    /**
     * Год внешнего сезона из активного сезона ({@code startDate} → год старта, напр. 2024/25 → 2024).
     */
    public String resolveExternalSeasonYear(Season season) {
        return resolveExternalSeasonYear(season, null);
    }

    /**
     * Год внешнего сезона из дат сезона (год старта, напр. 2024/25 → 2024; ЧМ-2026 с {@code startDate} 2026 → 2026).
     */
    public String resolveExternalSeasonYear(Season season, League.LeagueCode leagueCode) {
        Integer year = SeasonCalendarUtils.resolveExternalSeasonYear(season.getStartDate());
        if (year != null) {
            return String.valueOf(year);
        }
        if (properties.getDefaultSeason() != null && !properties.getDefaultSeason().isBlank()) {
            return properties.getDefaultSeason().trim();
        }
        throw new IllegalStateException("Cannot resolve external season year for season " + season.getId());
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

    public Optional<MatchdaySlotKey> buildMatchdayKey(League league, String matchDay, String externalSeason) {
        if (!LeagueCompetitionMapping.isSupported(league.getLeagueCode())) {
            return Optional.empty();
        }
        return resolveSlotOrder(league, matchDay)
                .map(order -> new MatchdaySlotKey(
                        league.getLeagueCode().name(),
                        order,
                        externalSeason,
                        league.getId()
                ));
    }
}
