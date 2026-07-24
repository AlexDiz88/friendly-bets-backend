package net.friendly_bets.tournamentarchive;

import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentArchiveTeamResolverTest {

    @Mock
    private TeamsRepository teamsRepository;

    private TournamentArchiveTeamResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TournamentArchiveTeamResolver(teamsRepository);
    }

    @Test
    void resolvesViaCountry() {
        when(teamsRepository.findByCountryIgnoreCase("MEX"))
                .thenReturn(Optional.of(Team.builder().id("mex-mongo-id").title("Mexico").country("MEX").build()));

        assertEquals("mex-mongo-id", resolver.resolveTeamId("mex"));
        assertTrue(resolver.unresolvedCodes().isEmpty());
    }

    @Test
    void resolvesSpainViaSpaCountryAlias() {
        when(teamsRepository.findByCountryIgnoreCase("ESP")).thenReturn(Optional.empty());
        when(teamsRepository.findByCountryIgnoreCase("SPA"))
                .thenReturn(Optional.of(Team.builder().id("esp-id").title("Spain").country("SPA").build()));

        assertEquals("esp-id", resolver.resolveTeamId("ESP"));
    }

    @Test
    void tracksUnresolved() {
        when(teamsRepository.findByCountryIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertNull(resolver.resolveTeamId("XXX"));
        assertTrue(resolver.unresolvedCodes().contains("XXX"));
    }
}
