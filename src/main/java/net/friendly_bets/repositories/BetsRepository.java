package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface BetsRepository extends MongoRepository<Bet, String> {

    boolean existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitle(
            User user,
            String matchDay,
            Team homeTeam,
            Team awayTeam,
            String betTitle
    );

}
