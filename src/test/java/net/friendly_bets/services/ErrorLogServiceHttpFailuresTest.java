package net.friendly_bets.services;

import net.friendly_bets.models.ErrorLog;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErrorLogServiceHttpFailuresTest {

    private ErrorLogRepository repository;
    private ErrorLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(ErrorLogRepository.class);
        service = new ErrorLogService(repository, null, null);
    }

    @Test
    void recordHttpRequestFailuresIfNeeded_skipsWhenAllSucceeded() {
        List<ExternalApiHttpLogEntry> logs = List.of(
                ExternalApiHttpLogEntry.builder().outcome("SUCCESS").build()
        );
        service.recordHttpRequestFailuresIfNeeded(
                ExternalDataLayer.SCHEDULE,
                "sports.ru",
                "EPL",
                "2025",
                logs,
                null
        );
        verify(repository, never()).save(any());
    }

    @Test
    void recordHttpRequestFailuresIfNeeded_logsBuiltMessageWhenNoSummary() {
        List<ExternalApiHttpLogEntry> logs = List.of(
                ExternalApiHttpLogEntry.builder().outcome("SUCCESS").requestType("SCHEDULE_PAGE").build(),
                ExternalApiHttpLogEntry.builder()
                        .outcome("HTTP_ERROR")
                        .requestType("MATCH_PAGE")
                        .detail("timeout")
                        .build()
        );
        when(repository.findFirstByProviderAndCodeAndLayerAndLeagueCodeAndMessage(
                eq("sports.ru"),
                eq(ErrorLogService.CODE_PROVIDER_FETCH_FAILED),
                eq("SCHEDULE"),
                eq("EPL"),
                any())).thenReturn(Optional.empty());

        service.recordHttpRequestFailuresIfNeeded(
                ExternalDataLayer.SCHEDULE,
                "sports.ru",
                "EPL",
                "2025",
                logs,
                null
        );

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        assertEquals(ErrorLogService.CODE_PROVIDER_FETCH_FAILED, captor.getValue().getCode());
        assertTrue(captor.getValue().getMessage().startsWith("httpSuccess=1/2"));
        assertTrue(captor.getValue().getMessage().contains("MATCH_PAGE:HTTP_ERROR"));
    }

    @Test
    void recordHttpRequestFailuresIfNeeded_skipsDuplicateProviderFetchFailed() {
        List<ExternalApiHttpLogEntry> logs = new ArrayList<>();
        logs.add(ExternalApiHttpLogEntry.builder().outcome("HTTP_ERROR").requestType("TOURNAMENT").build());
        String summary = "melbetHttpError";
        when(repository.findFirstByProviderAndCodeAndLayerAndLeagueCodeAndMessage(
                "melbet",
                ErrorLogService.CODE_PROVIDER_FETCH_FAILED,
                "ODDS",
                "EPL",
                summary)).thenReturn(Optional.of(ErrorLog.builder().id("existing").build()));

        service.recordHttpRequestFailuresIfNeeded(
                ExternalDataLayer.ODDS,
                "melbet",
                "EPL",
                "2025",
                logs,
                summary
        );

        verify(repository, never()).save(any());
    }

    @Test
    void recordFullMatchFailure_usesSpecificMessageAsCode() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .leagueCode("EPL")
                .matchday(1)
                .build();

        service.recordFullMatchFailure(match, "flashscorekz.com", "fullMatchNotFound");

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        assertEquals("fullMatchNotFound", captor.getValue().getCode());
        assertEquals("fullMatchNotFound", captor.getValue().getMessage());
        assertEquals("ms-1", captor.getValue().getMatchScheduleId());
        assertEquals(ExternalDataLayer.FULL_MATCH.name(), captor.getValue().getLayer());
    }

    @Test
    void recordFullMatchFailure_fallsBackWhenMessageBlank() {
        service.recordFullMatchFailure(MatchSchedule.builder().id("ms-2").build(), "soccer365.ru", "  ");

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        assertEquals(ErrorLogService.CODE_FULL_MATCH_FAILED, captor.getValue().getCode());
    }

    @Test
    void recordFullMatchFailure_appendsOccurrenceTimeOnDedupe() {
        Instant first = Instant.parse("2026-08-24T21:00:00Z");
        ErrorLog existing = ErrorLog.builder()
                .id("log-1")
                .createdAt(first)
                .firstOccurredAt(first)
                .lastOccurredAt(first)
                .occurredAt(new ArrayList<>(List.of(first)))
                .occurrenceCount(1)
                .code("fullMatchNotFound")
                .matchScheduleId("ms-1")
                .build();
        when(repository.findFirstByProviderAndCodeAndMatchScheduleId(
                "flashscorekz.com", "fullMatchNotFound", "ms-1")).thenReturn(Optional.of(existing));

        service.recordFullMatchFailure(
                MatchSchedule.builder().id("ms-1").build(),
                "flashscorekz.com",
                "fullMatchNotFound"
        );

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        ErrorLog saved = captor.getValue();
        assertEquals(first, saved.getCreatedAt());
        assertEquals(first, saved.getFirstOccurredAt());
        assertEquals(2, saved.getOccurrenceCount());
        assertEquals(2, saved.getOccurredAt().size());
        assertEquals(first, saved.getOccurredAt().get(0));
        assertEquals(saved.getLastOccurredAt(), saved.getOccurredAt().get(1));
    }
}
