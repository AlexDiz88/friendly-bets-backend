package net.friendly_bets.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDisplayNamesMigrationServiceTest {

    @Mock
    TeamsRepository teamsRepository;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    TeamDisplayNamesMigrationService service;

    @Test
    void migrate_fillsEmptyDisplayNamesFromBundledCatalog() {
        Team roma = Team.builder()
                .id("roma1")
                .title("Roma")
                .displayNames(null)
                .build();
        when(teamsRepository.findAll()).thenReturn(List.of(roma));
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.migrate();

        assertEquals(1, result.getScanned());
        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getAlreadyComplete());

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamsRepository).save(captor.capture());
        TeamDisplayNames names = captor.getValue().getDisplayNames();
        assertNotNull(names);
        assertEquals("Roma", names.getEn());
        assertEquals("Рома", names.getRu());
        assertEquals("AS Rom", names.getDe());
    }

    @Test
    void migrate_doesNotOverwriteExistingDisplayNames() {
        Team arsenal = Team.builder()
                .id("ars1")
                .title("Arsenal")
                .displayNames(TeamDisplayNames.builder().ru("Арсенал FC").build())
                .build();
        when(teamsRepository.findAll()).thenReturn(List.of(arsenal));
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.migrate();

        assertEquals(1, result.getUpdated());
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamsRepository).save(captor.capture());
        TeamDisplayNames names = captor.getValue().getDisplayNames();
        assertEquals("Арсенал FC", names.getRu());
        assertEquals("Arsenal", names.getEn());
    }

    @Test
    void migrate_skipsWhenAlreadyComplete() {
        Team arsenal = Team.builder()
                .id("ars1")
                .title("Arsenal")
                .displayNames(TeamDisplayNames.builder()
                        .en("Arsenal")
                        .ru("Арсенал")
                        .de("Arsenal")
                        .build())
                .build();
        when(teamsRepository.findAll()).thenReturn(List.of(arsenal));

        var result = service.migrate();

        assertEquals(1, result.getAlreadyComplete());
        assertEquals(0, result.getUpdated());
        verify(teamsRepository, never()).save(any());
    }
}
