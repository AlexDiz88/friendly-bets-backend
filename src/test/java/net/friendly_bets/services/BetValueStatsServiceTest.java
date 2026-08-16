package net.friendly_bets.services;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetValueRangeStats;
import net.friendly_bets.models.League;
import net.friendly_bets.models.PlayerStatsByBetValues;
import net.friendly_bets.models.enums.BetValueRange;
import net.friendly_bets.repositories.PlayerStatsByBetValuesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetValueStatsServiceTest {

    @Mock
    private PlayerStatsByBetValuesRepository playerStatsByBetValuesRepository;

    @InjectMocks
    private BetValueStatsService betValueStatsService;

    private final String seasonId = "seasonId";
    private final String leagueId = "leagueId";
    private final String userId = "userId";

    @Test
    @DisplayName("Should update league and total documents in the matching odds range")
    void calculateStatsByBetValues_ShouldUpdateLeagueAndTotal() {
        when(playerStatsByBetValuesRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.empty());
        when(playerStatsByBetValuesRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, TOTAL_ID, userId))
                .thenReturn(Optional.empty());

        Bet bet = Bet.builder()
                .betStatus(Bet.BetStatus.WON)
                .betOdds(2.0)
                .balanceChange(10.0)
                .build();

        betValueStatsService.calculateStatsByBetValues(seasonId, leagueId, League.LeagueCode.EPL, userId, bet, true);

        ArgumentCaptor<PlayerStatsByBetValues> captor = ArgumentCaptor.forClass(PlayerStatsByBetValues.class);
        verify(playerStatsByBetValuesRepository, times(2)).save(captor.capture());

        List<PlayerStatsByBetValues> saved = captor.getAllValues();
        PlayerStatsByBetValues leagueStats = saved.stream()
                .filter(stats -> leagueId.equals(stats.getLeagueId()))
                .findFirst()
                .orElseThrow();
        PlayerStatsByBetValues totalStats = saved.stream()
                .filter(stats -> TOTAL_ID.equals(stats.getLeagueId()))
                .findFirst()
                .orElseThrow();

        assertEquals(League.LeagueCode.EPL, leagueStats.getLeagueCode());
        assertNull(totalStats.getLeagueCode());
        assertMediumWon(leagueStats);
        assertMediumWon(totalStats);
    }

    @Test
    @DisplayName("Should reject settled bets without a positive odds value")
    void calculateStatsByBetValues_ShouldRejectMissingOdds() {
        when(playerStatsByBetValuesRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.empty());

        Bet bet = Bet.builder()
                .betStatus(Bet.BetStatus.WON)
                .betOdds(null)
                .balanceChange(10.0)
                .build();

        assertThrows(BadRequestException.class, () ->
                betValueStatsService.calculateStatsByBetValues(seasonId, leagueId, League.LeagueCode.EPL, userId, bet, true));
    }

    private void assertMediumWon(PlayerStatsByBetValues stats) {
        assertEquals(1, stats.getBetCount());
        assertEquals(10.0, stats.getActualBalance(), 0.001);

        BetValueRangeStats medium = stats.getRangeStats().stream()
                .filter(rangeStats -> rangeStats.getRange() == BetValueRange.MEDIUM)
                .findFirst()
                .orElseThrow();
        assertEquals(1, medium.getBetCount());
        assertEquals(1, medium.getWonBetCount());
        assertEquals(0, medium.getLostBetCount());
        assertEquals(10.0, medium.getActualBalance(), 0.001);
        assertEquals(2.0, medium.getAverageOdds(), 0.001);
    }
}
