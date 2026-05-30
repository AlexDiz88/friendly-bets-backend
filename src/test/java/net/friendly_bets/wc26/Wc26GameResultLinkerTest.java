package net.friendly_bets.wc26;

import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Wc26GameResultLinkerTest {

    @Mock
    GameResultRecordRepository gameResultRecordRepository;

    @Mock
    TeamAliasResolver teamAliasResolver;

    @InjectMocks
    Wc26GameResultLinker linker;

    @Test
    @DisplayName("findByScheduleId creates placeholder when teams resolve but no game_results yet")
    void findByScheduleId_createsPlaceholder() {
        when(gameResultRecordRepository.findByWc26ScheduleIdAndSeasonAndLeagueCode(
                1, "2026", League.LeagueCode.WC.name()))
                .thenReturn(Optional.empty());
        when(teamAliasResolver.resolveWc26Code("MEX"))
                .thenReturn(Optional.of(Team.builder().id("mex1").title("Mexico").build()));
        when(teamAliasResolver.resolveWc26Code("RSA"))
                .thenReturn(Optional.of(Team.builder().id("rsa1").title("SouthAfrica").build()));
        when(gameResultRecordRepository.findByLeagueCodeAndSeasonAndHomeTeamIdAndAwayTeamId(
                League.LeagueCode.WC.name(), "2026", "mex1", "rsa1"))
                .thenReturn(List.of());
        when(gameResultRecordRepository.save(any(GameResultRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<GameResultRecord> result = linker.findByScheduleId(1, "2026");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getWc26ScheduleId());
        assertEquals("mex1", result.get().getHomeTeamId());
        assertEquals("rsa1", result.get().getAwayTeamId());
        assertEquals("SCHEDULED", result.get().getStatus());

        ArgumentCaptor<GameResultRecord> captor = ArgumentCaptor.forClass(GameResultRecord.class);
        verify(gameResultRecordRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getMatchday());
    }

    @Test
    @DisplayName("findByScheduleId links existing game_results when teams resolve by title")
    void findByScheduleId_linksExistingGameResult() {
        when(gameResultRecordRepository.findByWc26ScheduleIdAndSeasonAndLeagueCode(
                2, "2026", League.LeagueCode.WC.name()))
                .thenReturn(Optional.empty());
        when(teamAliasResolver.resolveWc26Code("KOR"))
                .thenReturn(Optional.of(Team.builder().id("kor1").title("KoreaRepublic").build()));
        when(teamAliasResolver.resolveWc26Code("CZE"))
                .thenReturn(Optional.of(Team.builder().id("cze1").title("CzechRepublic").build()));
        GameResultRecord existing = GameResultRecord.builder()
                .id("gr1")
                .leagueCode(League.LeagueCode.WC.name())
                .season("2026")
                .homeTeamId("kor1")
                .awayTeamId("cze1")
                .build();
        when(gameResultRecordRepository.findByLeagueCodeAndSeasonAndHomeTeamIdAndAwayTeamId(
                League.LeagueCode.WC.name(), "2026", "kor1", "cze1"))
                .thenReturn(List.of(existing));
        when(gameResultRecordRepository.save(any(GameResultRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<GameResultRecord> result = linker.findByScheduleId(2, "2026");

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getWc26ScheduleId());
        assertEquals("gr1", result.get().getId());
    }

    @Test
    @DisplayName("findByScheduleId returns empty when team mapping is missing")
    void findByScheduleId_emptyWhenTeamsMissing() {
        when(gameResultRecordRepository.findByWc26ScheduleIdAndSeasonAndLeagueCode(
                1, "2026", League.LeagueCode.WC.name()))
                .thenReturn(Optional.empty());
        when(teamAliasResolver.resolveWc26Code("MEX")).thenReturn(Optional.empty());
        when(teamAliasResolver.resolveWc26Code("RSA"))
                .thenReturn(Optional.of(Team.builder().id("rsa1").title("SouthAfrica").build()));

        assertTrue(linker.findByScheduleId(1, "2026").isEmpty());
    }
}
