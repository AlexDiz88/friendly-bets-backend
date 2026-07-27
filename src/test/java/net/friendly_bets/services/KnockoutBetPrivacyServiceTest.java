package net.friendly_bets.services;

import net.friendly_bets.dto.BetDto;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnockoutBetPrivacyServiceTest {

    @Mock
    MatchScheduleRepository matchScheduleRepository;

    @Mock
    MatchdaySlotSupport matchdaySupport;

    KnockoutBetPrivacyService service;

    @BeforeEach
    void setUp() {
        service = new KnockoutBetPrivacyService(matchScheduleRepository, matchdaySupport);
    }

    @Test
    @DisplayName("masks other user's OPENED semi-final bet before kickoff")
    void masksOtherUserBetBeforeKickoff() {
        Bet bet = openedFinalBet("user-a", "home-1", "away-1");
        stubScheduledMatch(bet, Instant.now().plus(2, ChronoUnit.HOURS), 20);

        BetDto dto = service.toDto(bet, "user-b");

        assertTrue(Boolean.TRUE.equals(dto.getBetDetailsHidden()));
        assertNull(dto.getBetTitle());
        assertNull(dto.getBetOdds());
        assertNull(dto.getBetSize());
        assertNotNull(dto.getPlayer());
    }

    @Test
    @DisplayName("own bet stays visible before kickoff")
    void ownBetVisibleBeforeKickoff() {
        Bet bet = openedFinalBet("user-a", "home-1", "away-1");
        stubScheduledMatch(bet, Instant.now().plus(2, ChronoUnit.HOURS), 20);

        BetDto dto = service.toDto(bet, "user-a");

        assertFalse(Boolean.TRUE.equals(dto.getBetDetailsHidden()));
        assertNotNull(dto.getBetTitle());
        assertNotNull(dto.getBetOdds());
        assertEquals(10, dto.getBetSize());
    }

    @Test
    @DisplayName("reveals bet after kickoff")
    void revealsBetAfterKickoff() {
        Bet bet = openedFinalBet("user-a", "home-1", "away-1");
        stubScheduledMatch(bet, Instant.now().minus(5, ChronoUnit.MINUTES), 20);

        BetDto dto = service.toDto(bet, "user-b");

        assertFalse(Boolean.TRUE.equals(dto.getBetDetailsHidden()));
        assertNotNull(dto.getBetTitle());
    }

    @Test
    @DisplayName("reveals bet when match status is LIVE")
    void revealsBetWhenLive() {
        Bet bet = openedFinalBet("user-a", "home-1", "away-1");
        MatchSchedule match = scheduledMatch(Instant.now().plus(2, ChronoUnit.HOURS), 20);
        match.setStatus("LIVE");
        when(matchScheduleRepository.findByLeagueIdAndSeasonIdAndHomeTeamIdAndAwayTeamId(
                eq("league-1"), eq("season-1"), eq("home-1"), eq("away-1")))
                .thenReturn(List.of(match));
        when(matchdaySupport.resolveSlotOrder(bet.getLeague(), "final"))
                .thenReturn(Optional.of(20));

        BetDto dto = service.toDto(bet, "user-b");

        assertFalse(Boolean.TRUE.equals(dto.getBetDetailsHidden()));
    }

    @Test
    @DisplayName("shouldHideBetDetails false for non-sensitive CL quarter-final")
    void notHiddenForQuarterFinal() {
        Bet bet = openedBet("user-a", League.LeagueCode.CL, "1/4", "home-1", "away-1");

        assertFalse(service.shouldHideBetDetails(bet, "user-b"));
    }

    private Bet openedFinalBet(String userId, String homeTeamId, String awayTeamId) {
        return openedBet(userId, League.LeagueCode.WC, "final", homeTeamId, awayTeamId);
    }

    private Bet openedBet(String userId, League.LeagueCode leagueCode, String matchDay,
                          String homeTeamId, String awayTeamId) {
        User user = User.builder().id(userId).username(userId).build();
        Season season = Season.builder().id("season-1").build();
        League league = League.builder().id("league-1").leagueCode(leagueCode).build();
        Team home = Team.builder().id(homeTeamId).title("Home").build();
        Team away = Team.builder().id(awayTeamId).title("Away").build();
        BetTitle betTitle = BetTitle.builder().code((short) 1).label("П1").isNot(false).build();

        return Bet.builder()
                .id("bet-1")
                .user(user)
                .season(season)
                .league(league)
                .matchDay(matchDay)
                .homeTeam(home)
                .awayTeam(away)
                .betTitle(betTitle)
                .betOdds(2.5)
                .betSize(10)
                .betStatus(Bet.BetStatus.OPENED)
                .build();
    }

    private void stubScheduledMatch(Bet bet, Instant kickoffUtc, int slotOrder) {
        MatchSchedule match = scheduledMatch(kickoffUtc, slotOrder);
        when(matchScheduleRepository.findByLeagueIdAndSeasonIdAndHomeTeamIdAndAwayTeamId(
                eq("league-1"), eq("season-1"),
                eq(bet.getHomeTeam().getId()), eq(bet.getAwayTeam().getId())))
                .thenReturn(List.of(match));
        when(matchdaySupport.resolveSlotOrder(bet.getLeague(), "final"))
                .thenReturn(Optional.of(slotOrder));
    }

    private static MatchSchedule scheduledMatch(Instant kickoffUtc, int slotOrder) {
        return MatchSchedule.builder()
                .status("SCHEDULED")
                .utcKickoff(kickoffUtc)
                .matchday(slotOrder)
                .build();
    }
}
