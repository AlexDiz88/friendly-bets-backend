package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Wc26ScheduleLinker {

    private static final String WC_LEAGUE_CODE = "WC";

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;
    private final GameResultRecordRepository gameResultRecordRepository;

    public void linkIfNeeded(GameResultRecord record, Team homeTeam, Team awayTeam) {
        if (record == null || !WC_LEAGUE_CODE.equals(record.getLeagueCode()) || record.getWc26ScheduleId() != null) {
            return;
        }
        resolveScheduleId(homeTeam, awayTeam, record.primaryExternalSource())
                .ifPresent(record::setWc26ScheduleId);
    }

    public void backfillSeason(String season) {
        if (season == null || season.isBlank()) {
            return;
        }
        for (GameResultRecord record : gameResultRecordRepository.findByLeagueCodeAndSeason(WC_LEAGUE_CODE, season)) {
            if (record.getWc26ScheduleId() != null) {
                continue;
            }
            GameResultSourceSnapshot source = record.primaryExternalSource();
            if (source == null) {
                continue;
            }
            String homeFifa = fifaFromSide(source.getHome());
            String awayFifa = fifaFromSide(source.getAway());
            if (homeFifa == null || awayFifa == null) {
                continue;
            }
            wc26ScheduleMatchRepository.findByHomeFifaAndAwayFifa(homeFifa, awayFifa)
                    .ifPresent(match -> {
                        record.setWc26ScheduleId(match.getScheduleId());
                        gameResultRecordRepository.save(record);
                    });
        }
    }

    private Optional<Integer> resolveScheduleId(Team homeTeam, Team awayTeam, GameResultSourceSnapshot source) {
        String homeFifa = fifaFromTeam(homeTeam, source != null ? source.getHome() : null);
        String awayFifa = fifaFromTeam(awayTeam, source != null ? source.getAway() : null);
        if (homeFifa == null || awayFifa == null) {
            return Optional.empty();
        }
        return wc26ScheduleMatchRepository.findByHomeFifaAndAwayFifa(homeFifa, awayFifa)
                .map(match -> match.getScheduleId());
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
