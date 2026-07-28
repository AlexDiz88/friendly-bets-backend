package net.friendly_bets.football24;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.football24.config.Football24Properties;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.services.RunningSeasonLookup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.OptionalInt;

@Service
@RequiredArgsConstructor
public class Football24TeamNamesService {

    private final Football24Properties properties;
    private final Football24HttpClient httpClient;
    private final Football24ScheduleParser scheduleParser;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySlotSupport;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        int leagueId = requireLeagueId(leagueCode);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        String externalSeason = matchdaySlotSupport.resolveExternalSeasonYear(season, leagueCode);
        int seasonYear;
        try {
            seasonYear = Integer.parseInt(externalSeason);
        } catch (NumberFormatException e) {
            throw new BadRequestException("football24SeasonUnresolved");
        }

        String seasonsJson = httpClient.fetchSeasonsJson(leagueId);
        OptionalInt seasonIdOpt = scheduleParser.resolveSeasonId(seasonsJson, seasonYear);
        if (seasonIdOpt.isEmpty()) {
            throw new BadRequestException("football24SeasonUnresolved");
        }
        int seasonId = seasonIdOpt.getAsInt();
        String slug = properties.getTournamentSlugs().get(leagueCode.name());
        String fixturesJson = httpClient.fetchFixturesRoundsJson(seasonId, slug);
        List<String> names = scheduleParser.parseTeamNamesFromMatchday(fixturesJson, seasonId, 1);
        if (names.isEmpty()) {
            throw new BadRequestException("football24MatchdayOneTeamNamesEmpty");
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

    int requireLeagueId(League.LeagueCode leagueCode) {
        Integer id = properties.getLeagueIds().get(leagueCode.name());
        if (id == null || id <= 0) {
            throw new BadRequestException("football24LeagueNotConfigured");
        }
        if (!properties.getScheduleSyncLeagueCodes().contains(leagueCode.name())) {
            throw new BadRequestException("football24LeagueScheduleNotSupported");
        }
        return id;
    }
}
