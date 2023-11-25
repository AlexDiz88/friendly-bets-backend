package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface BetsRepository extends MongoRepository<Bet, String> {

    boolean existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
            User user,
            String matchDay,
            Team homeTeam,
            Team awayTeam,
            String betTitle,
            Double betOdds,
            Integer betSize
    );

    boolean existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
            User user,
            String matchDay,
            Team homeTeam,
            Team awayTeam,
            String betTitle,
            Double betOdds,
            Integer betSize,
            String gameResult,
            Bet.BetStatus betStatus
    );
}
