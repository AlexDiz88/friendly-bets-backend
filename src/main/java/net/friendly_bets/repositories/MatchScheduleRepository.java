package net.friendly_bets.repositories;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MatchScheduleRepository extends MongoRepository<MatchSchedule, String> {

    List<MatchSchedule> findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
            String leagueId,
            String seasonId,
            int matchday
    );

    Optional<MatchSchedule> findByLeagueIdAndSeasonIdAndMatchdayAndHomeTeamIdAndAwayTeamId(
            String leagueId,
            String seasonId,
            int matchday,
            String homeTeamId,
            String awayTeamId
    );
}
