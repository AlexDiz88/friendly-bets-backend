package net.friendly_bets.services;

import net.friendly_bets.models.ErrorLog;
import net.friendly_bets.repositories.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ErrorLogServiceTeamAliasMismatchTest {

    private ErrorLogRepository repository;
    private ErrorLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(ErrorLogRepository.class);
        service = new ErrorLogService(repository, null, null);
    }

    @Test
    void recordTeamAliasMismatchSummary_usesRussianMessageWhenKeptUnchanged() {
        service.recordTeamAliasMismatchSummary(
                "flashscorekz.com",
                "EPL",
                List.of(ErrorLogService.TeamAliasMismatchDetail.builder()
                        .teamTitle("Arsenal")
                        .currentAlias("Арсенал")
                        .incomingAlias("Arsenal")
                        .build()),
                false
        );

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        assertEquals(
                "Рассинхрон алиаса у 1 команды: алиасы оставлены без изменений. Arsenal: «Арсенал» → «Arsenal»",
                captor.getValue().getMessage()
        );
        assertTrue(captor.getValue().getContext().get("details").contains("«Арсенал» → «Arsenal»"));
    }

    @Test
    void recordTeamAliasMismatchSummary_usesRussianMessageWhenOverwritten() {
        service.recordTeamAliasMismatchSummary(
                "flashscorekz.com",
                "EPL",
                List.of(
                        ErrorLogService.TeamAliasMismatchDetail.builder()
                                .teamTitle("A")
                                .currentAlias("old")
                                .incomingAlias("new")
                                .build(),
                        ErrorLogService.TeamAliasMismatchDetail.builder()
                                .teamTitle("B")
                                .currentAlias("x")
                                .incomingAlias("y")
                                .build()
                ),
                true
        );

        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captor.capture());
        assertEquals(
                "Рассинхрон алиаса у 2 команд: алиасы перезаписаны при принудительной синхронизации. A: «old» → «new»; B: «x» → «y»",
                captor.getValue().getMessage()
        );
    }
}
