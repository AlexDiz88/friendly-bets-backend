package net.friendly_bets.repositories;

import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsersRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsById(String id);

    boolean existsByEmailEquals(String email);

}
