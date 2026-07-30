package net.friendly_bets.matchschedule;

import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleFarKickoffSkipSupportTest {

  private static final String SEASON_ID = "season-1";
  private static final String LEAGUE_ID = "league-1";
  private static final int CURRENT = 5;
  private static final int NEXT = 6;
  private static final int SKIP_DAYS = 3;

  @Mock
  private MatchScheduleRepository matchScheduleRepository;

  private ScheduleFarKickoffSkipSupport support;

  @BeforeEach
  void setUp() {
    support = new ScheduleFarKickoffSkipSupport(matchScheduleRepository);
  }

  @Test
  void skipsWhenCurrentIsCompleteAndFarAndNextHasAllKickoffs() {
    Instant farKickoff = Instant.now().plus(10, ChronoUnit.DAYS);
    stubMatchday(CURRENT, List.of(
        schedule(farKickoff),
        schedule(farKickoff.plus(1, ChronoUnit.HOURS))
    ));
    stubMatchday(NEXT, List.of(
        schedule(Instant.now().plus(17, ChronoUnit.DAYS)),
        schedule(Instant.now().plus(18, ChronoUnit.DAYS))
    ));

    assertTrue(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT, NEXT), SKIP_DAYS));
  }

  @Test
  void doesNotSkipWhenNextHasRowsWithoutUtcKickoff() {
    Instant farKickoff = Instant.now().plus(10, ChronoUnit.DAYS);
    stubMatchday(CURRENT, List.of(
        schedule(farKickoff),
        schedule(farKickoff.plus(1, ChronoUnit.HOURS))
    ));
    stubMatchday(NEXT, List.of(
        schedule(null),
        schedule(Instant.now().plus(17, ChronoUnit.DAYS))
    ));

    assertFalse(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT, NEXT), SKIP_DAYS));
  }

  @Test
  void doesNotSkipWhenNextHasNoRowsYet() {
    Instant farKickoff = Instant.now().plus(10, ChronoUnit.DAYS);
    stubMatchday(CURRENT, List.of(schedule(farKickoff)));
    stubMatchday(NEXT, List.of());

    assertFalse(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT, NEXT), SKIP_DAYS));
  }

  @Test
  void doesNotSkipWhenCurrentKickoffIsSoon() {
    Instant soonKickoff = Instant.now().plus(1, ChronoUnit.DAYS);
    stubMatchday(CURRENT, List.of(schedule(soonKickoff)));

    assertFalse(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT, NEXT), SKIP_DAYS));
  }

  @Test
  void doesNotSkipWhenCurrentHasMissingKickoff() {
    stubMatchday(CURRENT, List.of(schedule(null), schedule(Instant.now().plus(10, ChronoUnit.DAYS))));

    assertFalse(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT, NEXT), SKIP_DAYS));
  }

  @Test
  void skipsWhenOnlyCurrentIsInWindow() {
    Instant farKickoff = Instant.now().plus(10, ChronoUnit.DAYS);
    stubMatchday(CURRENT, List.of(schedule(farKickoff)));

    assertTrue(support.shouldSkip(SEASON_ID, LEAGUE_ID, CURRENT, window(CURRENT), SKIP_DAYS));
  }

  private void stubMatchday(int matchday, List<MatchSchedule> rows) {
    when(matchScheduleRepository.findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
        eq(LEAGUE_ID), eq(SEASON_ID), eq(matchday)))
        .thenReturn(rows);
  }

  private static Set<Integer> window(int... matchdays) {
    Set<Integer> window = new LinkedHashSet<>();
    for (int matchday : matchdays) {
      window.add(matchday);
    }
    return window;
  }

  private static MatchSchedule schedule(Instant kickoff) {
    return MatchSchedule.builder().utcKickoff(kickoff).build();
  }
}
