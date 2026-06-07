package net.friendly_bets.support;

import lombok.Builder;
import lombok.Getter;
import net.friendly_bets.models.*;

/**
 * Сохранённые сущности, созданные {@link TestDataFactory} для одного сценария теста.
 */
@Getter
@Builder(toBuilder = true)
public class TestFixture {

    private User moderator;
    private User player;
    private Season season;
    private League league;
    private Team homeTeam;
    private Team awayTeam;
    private CalendarNode calendarNode;
    private String matchDay;
    private Bet bet;
}
