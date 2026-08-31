package net.friendly_bets.sportsru;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.sportsru.config.SportsRuProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsRuTeamNamesService {

    private final SportsRuProperties properties;
    private final SportsRuHttpClient httpClient;
    private final SportsRuScheduleParser scheduleParser;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        requireScheduleSyncSupported(leagueCode);
        String calendarPath = requireCalendarPath(leagueCode);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String calendarHtml = httpClient.fetchCalendarHtml(calendarPath);
        List<String> names = scheduleParser.parseTeamNamesFromMatchday(calendarHtml, 1);
        if (names.isEmpty()) {
            String tablePath = calendarPath.replace("/calendar/", "/table/");
            if (!tablePath.equals(calendarPath)) {
                String tableHtml = httpClient.fetchCalendarHtml(tablePath);
                names = scheduleParser.parseTeamNamesFromTable(tableHtml);
            }
        }
        if (names.isEmpty()) {
            throw new BadRequestException("sportsRuMatchdayOneTeamNamesEmpty");
        }
        return names;
    }

    public boolean isScheduleSyncSupported(League.LeagueCode leagueCode) {
        if (leagueCode == null) {
            return false;
        }
        Set<String> enabled = properties.getScheduleSyncLeagueCodes();
        return enabled != null && enabled.contains(leagueCode.name());
    }

    public void requireScheduleSyncSupported(League.LeagueCode leagueCode) {
        if (!isScheduleSyncSupported(leagueCode)) {
            throw new BadRequestException("sportsRuLeagueScheduleNotSupported");
        }
    }

    public String requireCalendarPath(League.LeagueCode leagueCode) {
        String path = properties.getCalendarPaths() != null
                ? properties.getCalendarPaths().get(leagueCode.name())
                : null;
        if (path == null || path.isBlank()) {
            throw new BadRequestException("sportsRuCalendarPathNotConfigured");
        }
        return path.trim();
    }
}
