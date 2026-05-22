package net.friendly_bets.repositories;

import net.friendly_bets.models.external.ExternalMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalMatchRepository extends MongoRepository<ExternalMatch, String> {

    List<ExternalMatch> findByCompetitionCodeAndMatchdayAndSeason(
            String competitionCode, int matchday, String season);

    Optional<ExternalMatch> findByCompetitionCodeAndMatchdayAndSeasonAndHomeTeamIdAndAwayTeamId(
            String competitionCode, int matchday, String season, String homeTeamId, String awayTeamId);

    Optional<ExternalMatch> findByCompetitionCodeAndMatchdayAndSeasonAndExternalMatchId(
            String competitionCode, int matchday, String season, long externalMatchId);
}
