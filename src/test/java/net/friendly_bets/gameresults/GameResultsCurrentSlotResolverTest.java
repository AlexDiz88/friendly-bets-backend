package net.friendly_bets.gameresults;

import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TournamentFormatExpander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultsCurrentSlotResolverTest {

    @Mock
    private TournamentFormatExpander tournamentFormatExpander;

    @Mock
    private GameResultRecordRepository gameResultRecordRepository;

    @Mock
    private GameResultsSyncRepository gameResultsSyncRepository;

    @Mock
    private TeamsRepository teamsRepository;

    @InjectMocks
    private GameResultsCurrentSlotResolver resolver;

    private League league;
    private TournamentFormat format;

    @BeforeEach
    void setUp() {
        league = League.builder()
                .id("wc-league")
                .leagueCode(League.LeagueCode.WC)
                .build();
        format = TournamentFormat.builder().build();
        when(tournamentFormatExpander.expand(format)).thenReturn(List.of(
                berlinSlot("1 [1]", 1),
                berlinSlot("1 [4]", 4)
        ));
        when(gameResultsSyncRepository.findByLeagueCodeAndMatchdayAndSeason(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(teamsRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void resolveCurrentSlotOrder_skipsBerlinSlotWithEnoughFinalizedEvenIfExtraRecordsRemain() {
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), eq(1), eq("2026")))
                .thenReturn(List.of(
                        finalizedNamedRecord("Mexico", "South Africa"),
                        finalizedNamedRecord("Korea Republic", "Czechia"),
                        finalizedNamedRecord("Canada", "Bosnia and Herzegovina"),
                        finalizedNamedRecord("USA", "Paraguay"),
                        namedRecord("Mexico", "South Africa")
                ));
        when(gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(eq("WC"), eq(4), eq("2026")))
                .thenReturn(List.of(
                        finalizedNamedRecord("Spain", "Cape Verde Islands"),
                        finalizedNamedRecord("Belgium", "Egypt"),
                        finalizedNamedRecord("Saudi Arabia", "Uruguay"),
                        namedRecord("Iran", "New Zealand")
                ));

        int current = resolver.resolveCurrentSlotOrder(league, format, "2026");

        assertEquals(4, current);
    }

    private static ExpandedMatchdaySlot berlinSlot(String id, int order) {
        return ExpandedMatchdaySlot.builder()
                .id(id)
                .order(order)
                .kind(ExpandedMatchdaySlot.Kind.GROUP)
                .build();
    }

    private static GameResultRecord finalizedNamedRecord(String home, String away) {
        GameResultRecord record = namedRecord(home, away);
        record.setFinalizedAt(LocalDateTime.now());
        return record;
    }

    private static GameResultRecord namedRecord(String home, String away) {
        return GameResultRecord.builder()
                .sources(Map.of(
                        MatchDataProviders.FOURSCORE,
                        GameResultSourceSnapshot.builder()
                                .home(GameResultSideSnapshot.builder().externalName(home).build())
                                .away(GameResultSideSnapshot.builder().externalName(away).build())
                                .build()
                ))
                .build();
    }
}
