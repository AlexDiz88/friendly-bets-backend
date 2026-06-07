package net.friendly_bets.repositories;

import net.friendly_bets.models.AccountToken;
import net.friendly_bets.models.AccountTokenType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountTokenRepository extends MongoRepository<AccountToken, String> {

    Optional<AccountToken> findByTokenHashAndTypeAndUsedAtIsNull(String tokenHash, AccountTokenType type);

    void deleteByUserIdAndTypeAndUsedAtIsNull(String userId, AccountTokenType type);
}
