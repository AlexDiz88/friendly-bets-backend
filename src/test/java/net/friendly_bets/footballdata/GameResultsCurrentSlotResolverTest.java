package net.friendly_bets.footballdata;

import net.friendly_bets.models.League;
import net.friendly_bets.models.RoundRobinStage;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TournamentFormatExpander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultsCurrentSlotResolverTest {

    @Mock
    GameResultRecordRepository gameResultRecordRepository;
    @Mock
    GameResultsSyncRepository gameResultsSyncRepository;
    @Mock
    TeamsRepository teamsRepository;

    TournamentFormatExpander tournamentFormatExpander;
    GameResultsCurrentSlotResolver resolver;

    League wcLeague;
    TournamentFormat wcFormat;

    @BeforeEach
    void setUp() {
        tournamentFormatExpander = new TournamentFormatExpander();
        resolver = new GameResultsCurrentSlotResolver(
                tournamentFormatExpander,
                gameResultRecordRepository,
                gameResultsSyncRepository,
                teamsRepository
        );
        wcLeague = League.builder()
                .id("wc-league")
                .leagueCode(League.LeagueCode.WC)
                .tournamentFormatId("fmt-wc")
                .currentMatchDay("1 [4]")
                .build();
        wcFormat = TournamentFormat.builder()
                .formatCode("wc-48teams")
                .groupStage(RoundRobinStage.builder()
                        .matchdayCount(3)
                        .splitSlotsPerRound(true)
                        .slotsPerRound(List.of(6, 6, 4))
                        .build())
                .build();
    }

    @Test
    void returnsFirstSlotWhenNoResultsYet() {
        when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), anyInt(), eq("2026")))
                .thenReturn(Optional.empty());
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), anyInt(), eq("2026")))
                .thenReturn(List.of());

        assertEquals(1, resolver.resolveCurrentSlotOrder(wcLeague, wcFormat, "2026"));
    }

    @Test
    void returnsPollingSlotNotBettingSlot() {
        when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason("WC", 1, "2026"))
                .thenReturn(Optional.of(sync(1, GameResultsSyncStatus.COMPLETED)));
        when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason("WC", 2, "2026"))
                .thenReturn(Optional.of(sync(2, GameResultsSyncStatus.COMPLETED)));
        when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason("WC", 3, "2026"))
                .thenReturn(Optional.of(sync(3, GameResultsSyncStatus.POLLING)));
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), anyInt(), eq("2026")))
                .thenReturn(List.of());

        assertEquals(3, resolver.resolveCurrentSlotOrder(wcLeague, wcFormat, "2026"));
    }

    @Test
    void returnsLastSlotWhenAllComplete() {
        for (int order = 1; order <= 16; order++) {
            when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason("WC", order, "2026"))
                    .thenReturn(Optional.of(sync(order, GameResultsSyncStatus.COMPLETED)));
        }
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), anyInt(), eq("2026")))
                .thenReturn(List.of());

        assertEquals(16, resolver.resolveCurrentSlotOrder(wcLeague, wcFormat, "2026"));
    }

    private static GameResultsSync sync(int matchday, GameResultsSyncStatus status) {
        return GameResultsSync.builder()
                .leagueCode("WC")
                .matchday(matchday)
                .season("2026")
                .syncStatus(status)
                .build();
    }
}
