package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Wc26ScheduleLinker {

    private static final String WC_LEAGUE_CODE = "WC";

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final TeamsRepository teamsRepository;
    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;

    /**
     * По паре сборных из wc26_schedule: schedule_id + kickoff FIFA (Berlin → UTC).
     */
    public void linkIfNeeded(GameResultRecord record, Team homeTeam, Team awayTeam) {
        if (record == null || !WC_LEAGUE_CODE.equals(record.getLeagueCode())) {
            return;
        }
        resolveScheduleId(homeTeam, awayTeam, record.primaryExternalSource())
                .ifPresent(scheduleId -> {
                    record.setWc26ScheduleId(scheduleId);
                    backfillKickoffFromSchedule(record);
                });
    }

    /**
     * Kickoff для UI и слотов — всегда из wc26_schedule (FIFA Berlin), не из 4score MSK.
     */
    public void backfillKickoffFromSchedule(GameResultRecord record) {
        if (record == null || record.getWc26ScheduleId() == null) {
            return;
        }
        if (!WC_LEAGUE_CODE.equals(record.getLeagueCode())) {
            return;
        }
        wc26ScheduleKickoffResolver.kickoffUtc(record.getWc26ScheduleId())
                .ifPresent(record::setUtcDate);
    }

    public void backfillSeason(String season) {
        if (season == null || season.isBlank()) {
            return;
        }
        for (GameResultRecord record : gameResultRecordRepository.findByLeagueCodeAndSeason(WC_LEAGUE_CODE, season)) {
            GameResultSourceSnapshot source = record.primaryExternalSource();
            if (source == null) {
                continue;
            }
            Optional<Team> home = loadTeam(record.getHomeTeamId());
            Optional<Team> away = loadTeam(record.getAwayTeamId());
            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }
            resolveScheduleId(home.get(), away.get(), source).ifPresent(id -> {
                boolean idChanged = !Integer.valueOf(id).equals(record.getWc26ScheduleId());
                record.setWc26ScheduleId(id);
                LocalDateTime before = record.getUtcDate();
                backfillKickoffFromSchedule(record);
                if (idChanged || (record.getUtcDate() != null && !record.getUtcDate().equals(before))) {
                    gameResultRecordRepository.save(record);
                }
            });
        }
    }

    public void backfillKickoffSeason(String season) {
        if (season == null || season.isBlank()) {
            return;
        }
        for (GameResultRecord record : gameResultRecordRepository.findByLeagueCodeAndSeason(WC_LEAGUE_CODE, season)) {
            if (record.getWc26ScheduleId() == null) {
                continue;
            }
            LocalDateTime before = record.getUtcDate();
            backfillKickoffFromSchedule(record);
            if (record.getUtcDate() != null && !record.getUtcDate().equals(before)) {
                gameResultRecordRepository.save(record);
            }
        }
    }

    /**
     * Проставить wc26ScheduleId записям тура, если пара уже есть в FIFA-расписании.
     */
    public void relinkMatchdayRecords(List<GameResultRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (GameResultRecord record : records) {
            if (!WC_LEAGUE_CODE.equals(record.getLeagueCode()) || record.getWc26ScheduleId() != null) {
                continue;
            }
            Optional<Team> home = loadTeam(record.getHomeTeamId());
            Optional<Team> away = loadTeam(record.getAwayTeamId());
            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }
            linkIfNeeded(record, home.get(), away.get());
            if (record.getWc26ScheduleId() != null) {
                gameResultRecordRepository.save(record);
            }
        }
    }

    private Optional<Team> loadTeam(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findById(teamId);
    }

    private Optional<Integer> resolveScheduleId(Team homeTeam, Team awayTeam, GameResultSourceSnapshot source) {
        String homeFifa = fifaFromTeam(homeTeam, source != null ? source.getHome() : null);
        String awayFifa = fifaFromTeam(awayTeam, source != null ? source.getAway() : null);
        if (homeFifa == null || awayFifa == null) {
            return Optional.empty();
        }
        Optional<Wc26ScheduleMatch> fromDb = wc26ScheduleMatchRepository.findByHomeFifaAndAwayFifa(homeFifa, awayFifa);
        if (fromDb.isPresent()) {
            return Optional.of(fromDb.get().getScheduleId());
        }
        return Wc26ScheduleCatalog.findByTeamPair(homeFifa, awayFifa).map(Wc26ScheduleCatalog.GroupMatch::scheduleId);
    }

    private static String fifaFromTeam(Team team, GameResultSideSnapshot side) {
        if (team != null && team.getTitle() != null) {
            Optional<String> fromTitle = Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle());
            if (fromTitle.isPresent()) {
                return fromTitle.get();
            }
        }
        if (team != null && team.getCountry() != null && team.getCountry().length() == 3) {
            return team.getCountry().toUpperCase();
        }
        return fifaFromSide(side);
    }

    private static String fifaFromSide(GameResultSideSnapshot side) {
        if (side == null) {
            return null;
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(side.getExternalName()).orElse(null);
    }
}
