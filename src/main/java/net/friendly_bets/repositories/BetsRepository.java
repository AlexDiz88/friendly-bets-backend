package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.League;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface BetsRepository extends MongoRepository<Bet, String> {

    boolean existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
            String seasonId,
            String leagueId,
            String userId,
            String matchDay,
            String homeTeamId,
            String awayTeamId,
            List<Bet.BetStatus> betStatuses
    );

    boolean existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetTitle_CodeAndBetTitle_IsNotAndBetOddsAndBetSizeAndGameScoreAndBetStatus(
            String seasonId,
            String leagueId,
            String userId,
            String matchDay,
            String homeTeamId,
            String awayTeamId,
            Short code,
            Boolean isNot,
            Double betOdds,
            Integer betSize,
            GameScore gameScore,
            Bet.BetStatus betStatus
    );

    int countBetsByLeagueAndBetStatusNot(League league, Bet.BetStatus betStatus);

    List<Bet> findAllBySeason_Id(String seasonId);

    List<Bet> findAllBySeason_IdAndBetStatus(String seasonId, Bet.BetStatus betStatus);

    Page<Bet> findAllBySeason_IdAndBetStatusIn(String seasonId, List<Bet.BetStatus> betStatuses, Pageable pageable);

    Page<Bet> findAllBySeason_IdAndBetStatusInAndLeague_IdAndUser_Id(String seasonId, List<Bet.BetStatus> betStatuses, String leagueId, String userId, Pageable pageable);

    Page<Bet> findAllBySeason_IdAndBetStatusInAndLeague_Id(String seasonId, List<Bet.BetStatus> betStatuses, String leagueId, Pageable pageable);

    Page<Bet> findAllBySeason_IdAndBetStatusInAndUser_Id(String seasonId, List<Bet.BetStatus> betStatuses, String userId, Pageable pageable);

    @Query("SELECT b FROM Bet b WHERE b.season.id = :seasonId " +
            "AND b.betStatus IN :betStatuses " +
            "AND (:playerId IS NULL OR b.user.id = :playerId) " +
            "AND (:leagueId IS NULL OR b.league.id = :leagueId) " +
            "ORDER BY COALESCE(b.betResultAddedAt, b.createdAt) DESC")
    Page<Bet> findAllBySeasonIdAndBetStatusIn(
            @Param("seasonId") String seasonId,
            @Param("betStatuses") List<Bet.BetStatus> betStatuses,
            @Param("playerId") String playerId,
            @Param("leagueId") String leagueId,
            Pageable pageable);
}
