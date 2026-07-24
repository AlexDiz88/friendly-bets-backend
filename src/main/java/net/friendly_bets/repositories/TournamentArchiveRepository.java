package net.friendly_bets.repositories;

import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TournamentArchiveRepository extends MongoRepository<TournamentArchive, String> {

    Optional<TournamentArchive> findByEditionCode(String editionCode);

    boolean existsByEditionCode(String editionCode);
}
