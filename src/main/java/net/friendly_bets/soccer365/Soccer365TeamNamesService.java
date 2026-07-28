package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Soccer365TeamNamesService {

    private final Soccer365Properties properties;
    private final Soccer365HttpClient httpClient;
    private final Soccer365ScheduleParser scheduleParser;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        int competitionId = requireCompetitionId(leagueCode);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        String html = httpClient.fetchScheduleHtml(competitionId);
        List<String> names = scheduleParser.parseTeamNamesFromMatchday(html, competitionId, 1);
        if (names.isEmpty()) {
            throw new BadRequestException("soccer365MatchdayOneTeamNamesEmpty");
        }
        return names;
    }

    public static League.LeagueCode parseLeagueCode(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }

    int requireCompetitionId(League.LeagueCode leagueCode) {
        Integer id = properties.getCompetitionIds().get(leagueCode.name());
        if (id == null || id <= 0) {
            throw new BadRequestException("soccer365CompetitionNotConfigured");
        }
        return id;
    }
}
