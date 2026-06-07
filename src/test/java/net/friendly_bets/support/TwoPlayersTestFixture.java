package net.friendly_bets.support;

import lombok.Builder;
import lombok.Getter;
import net.friendly_bets.models.*;

@Getter
@Builder(toBuilder = true)
public class TwoPlayersTestFixture {

    private User moderator;
    private User playerOne;
    private User playerTwo;
    private Season season;
    private League league;
    private Team homeTeam;
    private Team awayTeam;
    private CalendarNode calendarNode;
    private String matchDay;
    /** Второй тур в том же календарном узле (gameweek), если задан. */
    private String secondMatchDay;
    /** Третья команда в лиге (для сценариев смены соперника). */
    private Team thirdTeam;
}
