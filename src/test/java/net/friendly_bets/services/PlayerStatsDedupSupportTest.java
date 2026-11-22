package net.friendly_bets.services;

import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.repositories.PlayerStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerStatsDedupSupportTest {

    @Mock
    PlayerStatsRepository repository;

    @Test
    void pickOne_returnsNullWhenEmpty() {
        assertNull(PlayerStatsDedupSupport.pickOne(List.of(), "s", "total", "u"));
    }

    @Test
    void pickOne_doesNotDeleteWhenDuplicates() {
        PlayerStats weak = PlayerStats.builder().id("a").betCount(1).totalBets(1).build();
        PlayerStats strong = PlayerStats.builder().id("b").betCount(5).totalBets(8).build();

        PlayerStats kept = PlayerStatsDedupSupport.pickOne(
                List.of(weak, strong),
                "season",
                "total",
                "user"
        );

        assertEquals("b", kept.getId());
        verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteExtras_removesNonCanonical() {
        PlayerStats weak = PlayerStats.builder().id("a").betCount(1).totalBets(1).build();
        PlayerStats strong = PlayerStats.builder().id("b").betCount(5).totalBets(8).build();
        PlayerStats mid = PlayerStats.builder().id("c").betCount(3).totalBets(4).build();

        int removed = PlayerStatsDedupSupport.deleteExtras(
                repository,
                List.of(weak, strong, mid)
        );

        assertEquals(2, removed);
        verify(repository).deleteById("a");
        verify(repository).deleteById("c");
        verify(repository, never()).deleteById("b");
    }
}
