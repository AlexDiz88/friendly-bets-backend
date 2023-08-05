package net.friendly_bets.repositories;

import net.friendly_bets.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsById(Long id);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);



}
