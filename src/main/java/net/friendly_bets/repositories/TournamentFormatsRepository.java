package net.friendly_bets.repositories;

import net.friendly_bets.models.TournamentFormat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TournamentFormatsRepository extends MongoRepository<TournamentFormat, String> {

    boolean existsByFormatCode(String formatCode);

    Optional<TournamentFormat> findByFormatCode(String formatCode);
}
