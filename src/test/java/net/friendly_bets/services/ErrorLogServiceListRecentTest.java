package net.friendly_bets.services;

import net.friendly_bets.dto.ErrorLogDto;
import net.friendly_bets.models.ErrorLog;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.ErrorLogRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorLogServiceListRecentTest {

    private ErrorLogRepository errorLogRepository;
    private MatchScheduleRepository matchScheduleRepository;
    private TeamsRepository teamsRepository;
    private ErrorLogService service;

    @BeforeEach
    void setUp() {
        errorLogRepository = mock(ErrorLogRepository.class);
        matchScheduleRepository = mock(MatchScheduleRepository.class);
        teamsRepository = mock(TeamsRepository.class);
        service = new ErrorLogService(errorLogRepository, matchScheduleRepository, teamsRepository);
    }

    @Test
    void listRecent_fillsTeamTitlesAndLogoKeysFromMatchSchedule() {
        ErrorLog log = ErrorLog.builder()
                .id("log-1")
                .createdAt(Instant.parse("2026-08-25T00:00:00Z"))
                .code("fullMatchNotFound")
                .matchScheduleId("ms-1")
                .leagueCode("EPL")
                .build();
        MatchSchedule schedule = MatchSchedule.builder()
                .id("ms-1")
                .homeTeamId("home-1")
                .awayTeamId("away-1")
                .build();
        Team home = Team.builder().id("home-1").title("Arsenal").logo("arsenal").build();
        Team away = Team.builder().id("away-1").title("Chelsea").logo("chelsea").build();

        when(errorLogRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        when(matchScheduleRepository.findAllById(any())).thenReturn(List.of(schedule));
        when(teamsRepository.findAllById(any())).thenReturn(List.of(home, away));

        List<ErrorLogDto> result = service.listRecent();

        assertEquals(1, result.size());
        ErrorLogDto dto = result.get(0);
        assertEquals("Arsenal", dto.getHomeTeamTitle());
        assertEquals("Chelsea", dto.getAwayTeamTitle());
        assertEquals("arsenal", dto.getHomeTeamLogoKey());
        assertEquals("chelsea", dto.getAwayTeamLogoKey());
        assertEquals("Arsenal", dto.getHomeTeam());
        assertEquals("Chelsea", dto.getAwayTeam());
    }

    @Test
    void listRecent_keepsStoredTeamNamesWhenAlreadyPresent() {
        ErrorLog log = ErrorLog.builder()
                .id("log-2")
                .createdAt(Instant.parse("2026-08-25T00:00:00Z"))
                .code("fullMatchFailed")
                .matchScheduleId("ms-2")
                .homeTeam("stored-home")
                .awayTeam("stored-away")
                .build();
        MatchSchedule schedule = MatchSchedule.builder()
                .id("ms-2")
                .homeTeamId("home-2")
                .awayTeamId("away-2")
                .build();
        Team home = Team.builder().id("home-2").title("Liverpool").logo("liverpool").build();
        Team away = Team.builder().id("away-2").title("Everton").logo("everton").build();

        when(errorLogRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        when(matchScheduleRepository.findAllById(any())).thenReturn(List.of(schedule));
        when(teamsRepository.findAllById(any())).thenReturn(List.of(home, away));

        ErrorLogDto dto = service.listRecent().get(0);
        assertEquals("stored-home", dto.getHomeTeam());
        assertEquals("stored-away", dto.getAwayTeam());
        assertEquals("Liverpool", dto.getHomeTeamTitle());
        assertEquals("Everton", dto.getAwayTeamTitle());
    }

    @Test
    void listRecent_skipsEnrichmentWithoutMatchScheduleId() {
        ErrorLog log = ErrorLog.builder()
                .id("log-3")
                .createdAt(Instant.parse("2026-08-25T00:00:00Z"))
                .code("teamMappingMissing")
                .homeTeam("Unknown FC")
                .build();
        when(errorLogRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        ErrorLogDto dto = service.listRecent().get(0);
        assertEquals("Unknown FC", dto.getHomeTeam());
        assertNull(dto.getHomeTeamTitle());
        assertNull(dto.getHomeTeamLogoKey());
    }
}
