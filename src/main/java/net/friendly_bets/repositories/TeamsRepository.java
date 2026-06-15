package net.friendly_bets.repositories;

import net.friendly_bets.models.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;


public interface TeamsRepository extends MongoRepository<Team, String> {

    boolean existsByTitle(String title);

    Optional<Team> findByTitleIgnoreCase(String title);

    Optional<Team> findByTitle(String title);

    @Query("{ 'external_aliases': { $elemMatch: { 'provider': ?0, 'external_id': ?1 } } }")
    Optional<Team> findByExternalAliasId(String provider, int externalId);

    @Query("{ 'external_aliases': { $elemMatch: { 'provider': ?0, 'external_name': ?1 } } }")
    Optional<Team> findByExternalAliasName(String provider, String externalName);
}
