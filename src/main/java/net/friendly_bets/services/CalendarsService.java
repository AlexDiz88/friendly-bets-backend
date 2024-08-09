package net.friendly_bets.services;

import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.CalendarNodeDto;
import net.friendly_bets.dto.CalendarNodesPage;
import net.friendly_bets.dto.NewCalendarNodeDto;

public interface CalendarsService {

    CalendarNodesPage getAllSeasonCalendarNodes(String seasonId);

    CalendarNodesPage getSeasonCalendarHasBetsNodes(String seasonId);

    BetsPage getActualCalendarNodeBets(String seasonId);

    CalendarNodeDto createCalendarNode(NewCalendarNodeDto newCalendarNode);

    CalendarNodeDto addBetToCalendarNode(String betId, String calendarNodeId, String leagueId);

    BetsPage getBetsByCalendarNode(String calendarNodeId);

    CalendarNodeDto deleteCalendarNode(String calendarNodeId);

    CalendarNodeDto deleteBetInCalendarNode(String calendarNodeId, String betId);

    CalendarNodeDto deleteBetInCalendars(String seasonId, String betId);
}
