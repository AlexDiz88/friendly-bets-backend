package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Wc26GameResultLinker {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final TeamAliasResolver teamAliasResolver;

    public Optional<GameResultRecord> linkIfPossible(GameResultRecord record, String storageSeason) {
        if (record == null || record.getWc26ScheduleId() != null) {
            return Optional.ofNullable(record);
        }
        if (!League.LeagueCode.WC.name().equals(record.getLeagueCode())) {
            return Optional.of(record);
        }
        if (record.getHomeTeamId() == null || record.getAwayTeamId() == null) {
            return Optional.of(record);
        }
        Optional<Integer> scheduleId = resolveScheduleId(record.getHomeTeamId(), record.getAwayTeamId());
        if (scheduleId.isEmpty()) {
            return Optional.of(record);
        }
        record.setWc26ScheduleId(scheduleId.get());
        return Optional.of(gameResultRecordRepository.save(record));
    }

    public Optional<GameResultRecord> findByScheduleId(int scheduleId, String storageSeason) {
        Optional<GameResultRecord> byField = gameResultRecordRepository.findByWc26ScheduleIdAndSeasonAndLeagueCode(
                scheduleId, storageSeason, League.LeagueCode.WC.name());
        if (byField.isPresent()) {
            return byField;
        }
        return findAndLinkByTeams(scheduleId, storageSeason);
    }

    private Optional<GameResultRecord> findAndLinkByTeams(int scheduleId, String storageSeason) {
        Optional<Wc26ScheduleCatalog.Wc26ScheduleEntry> entry = Wc26ScheduleCatalog.find(scheduleId);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> homeId = teamIdForCode(entry.get().homeCode());
        Optional<String> awayId = teamIdForCode(entry.get().awayCode());
        if (homeId.isEmpty() || awayId.isEmpty()) {
            return Optional.empty();
        }
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndSeasonAndHomeTeamIdAndAwayTeamId(
                League.LeagueCode.WC.name(),
                storageSeason,
                homeId.get(),
                awayId.get()
        );
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        GameResultRecord record = matches.get(0);
        record.setWc26ScheduleId(scheduleId);
        return Optional.of(gameResultRecordRepository.save(record));
    }

    private Optional<Integer> resolveScheduleId(String homeTeamId, String awayTeamId) {
        for (int id = 1; id <= 72; id++) {
            Optional<Wc26ScheduleCatalog.Wc26ScheduleEntry> entry = Wc26ScheduleCatalog.find(id);
            if (entry.isEmpty()) {
                continue;
            }
            Optional<String> home = teamIdForCode(entry.get().homeCode());
            Optional<String> away = teamIdForCode(entry.get().awayCode());
            if (home.isPresent() && away.isPresent()
                    && home.get().equals(homeTeamId) && away.get().equals(awayTeamId)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private Optional<String> teamIdForCode(String code) {
        return teamAliasResolver.resolveWc26Code(code).map(Team::getId);
    }
}
