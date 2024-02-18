package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;


public interface BetsRepository extends MongoRepository<Bet, String> {

    boolean existsByUserAndMatchDayAndPlayoffRoundAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
            User user,
            String matchDay,
            String playoffRound,
            Team homeTeam,
            Team awayTeam,
            String betTitle,
            Double betOdds,
            Integer betSize
    );

    boolean existsBySeasonIdAndLeagueIdAndUserAndMatchDayAndPlayoffRoundAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
            String seasonId,
            String leagueId,
            User user,
            String matchDay,
            String playoffRound,
            Team homeTeam,
            Team awayTeam,
            String betTitle,
            Double betOdds,
            Integer betSize
    );

    boolean existsByUserAndMatchDayAndPlayoffRoundAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
            User user,
            String matchDay,
            String playoffRound,
            Team homeTeam,
            Team awayTeam,
            String betTitle,
            Double betOdds,
            Integer betSize,
            String gameResult,
            Bet.BetStatus betStatus
    );

    int countBetsByLeagueAndBetStatusNot(League league, Bet.BetStatus betStatus);

    List<Bet> findAllBySeason_Id(String seasonId);

    List<Bet> findAllBySeason_IdAndBetStatus(String seasonId, Bet.BetStatus betStatus);

    Page<Bet> findAllByBetStatusIn(List<Bet.BetStatus> betStatuses, Pageable pageable);

    Page<Bet> findAllByBetStatusInAndLeague_IdAndUser_Id(List<Bet.BetStatus> betStatuses, String leagueId, String userId, Pageable pageable);

    Page<Bet> findAllByBetStatusInAndLeague_Id(List<Bet.BetStatus> betStatuses, String leagueId, Pageable pageable);

    Page<Bet> findAllByBetStatusInAndUser_Id(List<Bet.BetStatus> betStatuses, String userId, Pageable pageable);

    List<Bet> findAllByUser_IdAndLeague_IdAndBetStatusIn(String userId, String leagueId, List<Bet.BetStatus> betStatuses, Pageable pageable);

    @Query("{ 'user.username' : ?0, 'league.displayNameRu' : ?1, 'betStatus' : { $in : ?2 } }")
    List<Bet> findAllByUser_UsernameAndLeague_DisplayNameRuAndBetStatusIn(String username, String leagueName, List<Bet.BetStatus> betStatuses, Pageable pageable);
}
