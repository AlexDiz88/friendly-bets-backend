package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.marathonbet.mapping.MarathonbetBetTitleMapper;
import net.friendly_bets.models.League;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.OddsMergedOddsService;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.repositories.MarathonbetSyncRunRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.GetEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarathonbetSyncServiceTest {

    @Mock
    MarathonbetProperties properties;
    @Mock
    MarathonbetTournamentClient tournamentClient;
    @Mock
    MarathonbetScrapeService scrapeService;
    @Mock
    MarathonbetEventMatcher eventMatcher;
    @Mock
    MarathonbetBetTitleMapper betTitleMapper;
    @Mock
    OddsMergedOddsService oddsMergedOddsService;
    @Mock
    FootballDataSyncService footballDataSyncService;
    @Mock
    FootballDataCompetitionService footballDataCompetitionService;
    @Mock
    FootballDataMatchdaySupport matchdaySupport;
    @Mock
    SeasonsRepository seasonsRepository;
    @Mock
    GetEntityService getEntityService;
    @Mock
    ApiSyncIssueService apiSyncIssueService;
    @Mock
    MarathonbetSyncRunRepository syncRunRepository;

    @InjectMocks
    MarathonbetSyncService syncService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(properties.isSyncEnabled()).thenReturn(true);
        when(properties.getSseDelayMs()).thenReturn(0L);
        when(properties.getTournamentTreeIds()).thenReturn(java.util.Map.of("WC", 2_253_726L));
    }

    @Test
    void syncSlot_alwaysFetchesSseOncePerPendingMatch() throws Exception {
        League league = League.builder()
                .id("wc-league")
                .leagueCode(League.LeagueCode.WC)
                .build();
        when(getEntityService.getLeagueOrThrow("wc-league")).thenReturn(league);
        when(tournamentClient.fetchTournament(2_253_726L))
                .thenReturn(objectMapper.readTree("{\"prematchEvents\":[]}"));

        GameResultRecord match = GameResultRecord.builder()
                .id("gr-1")
                .status("SCHEDULED")
                .utcDate(LocalDateTime.now().plusDays(1))
                .build();
        when(footballDataSyncService.getMatches(eq("WC"), eq(3), any(), eq("wc-league")))
                .thenReturn(List.of(match));

        MarathonbetPrematchEvent event = MarathonbetPrematchEvent.builder()
                .treeId(25_819_358L)
                .homeTeam("Мексика")
                .awayTeam("ЮАР")
                .build();
        when(eventMatcher.resolveAndPersistTreeId(eq(match), any(), eq("WC"), any(), eq(3)))
                .thenReturn(Optional.of(event));

        when(scrapeService.fetchEventSnapshot(25_819_358L))
                .thenReturn(objectMapper.readTree("{\"markets\":{}}"));
        when(betTitleMapper.map(any(), eq("Мексика"), eq("ЮАР")))
                .thenReturn(List.of(MappedOddsQuote.builder().bookmaker("marathonbet").build()));
        when(oddsMergedOddsService.buildAndPersistFromQuotes(any(), any(), any(), any(), eq(false)))
                .thenReturn(OddsMergeResult.builder().marketGroups(List.of()).build());

        MarathonbetSyncResult result = syncService.syncSlot("wc-league", 3, "2026", null);

        verify(scrapeService).fetchEventSnapshot(25_819_358L);
        assertEquals(1, result.getSseCalls());
        assertEquals(1, result.getMatchesMatched());
    }
}
