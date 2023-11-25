package net.friendly_bets.repositories;

import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsersRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsById(String id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);




}
