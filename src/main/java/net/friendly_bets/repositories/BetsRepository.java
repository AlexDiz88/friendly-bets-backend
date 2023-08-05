package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetsRepository extends JpaRepository<Bet, Long> {

    boolean existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitle(
            User user,
            String matchDay,
            Team homeTeam,
            Team awayTeam,
            String betTitle
    );

}
