package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FootballDataMatchdaySupportTest {

    @Mock
    private FootballDataProperties properties;

    @Mock
    private GetEntityService getEntityService;

    @Mock
    private TournamentFormatExpander tournamentFormatExpander;

    @InjectMocks
    private FootballDataMatchdaySupport support;

    @Test
    void resolvesClubSeasonByStartYear() {
        Season season = Season.builder()
                .startDate(LocalDate.of(2024, 8, 16))
                .endDate(LocalDate.of(2025, 5, 25))
                .build();

        assertEquals("2024", support.resolveFootballDataSeasonYear(season, League.LeagueCode.EPL));
    }

    @Test
    void resolvesWcSeasonByEndYear() {
        Season season = Season.builder()
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .build();

        assertEquals("2026", support.resolveFootballDataSeasonYear(season, League.LeagueCode.WC));
    }
}
