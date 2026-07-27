package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.LeagueCodePathSupport;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.utils.SeasonCalendarUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchScheduleQueryService {

    private final MatchScheduleRepository matchScheduleRepository;
    private final SeasonsRepository seasonsRepository;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<MatchSchedule> getMatches(
            String pathLeagueOrCompetitionCode,
            int matchday,
            String seasonYear,
            String leagueId
    ) {
        Season season = resolveSeason(seasonYear);
        if (leagueId != null && !leagueId.isBlank()) {
            return matchScheduleRepository.findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                    leagueId.trim(),
                    season.getId(),
                    matchday
            );
        }
        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);
        return matchScheduleRepository.findByLeagueCodeAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                leagueCode,
                season.getId(),
                matchday
        );
    }

    public Season resolveSeason(String seasonYear) {
        if (seasonYear == null || seasonYear.isBlank()) {
            return runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        }
        String year = seasonYear.trim();
        Optional<Season> running = runningSeasonLookup.findRunningSeason();
        if (running.isPresent() && yearMatches(running.get(), year)) {
            return running.get();
        }
        return seasonsRepository.findAll().stream()
                .filter(s -> yearMatches(s, year))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("noActiveSeasonWasFounded"));
    }

    private static boolean yearMatches(Season season, String year) {
        Integer resolved = SeasonCalendarUtils.resolveExternalSeasonYear(season.getStartDate());
        return resolved != null && Objects.equals(String.valueOf(resolved), year);
    }
}
