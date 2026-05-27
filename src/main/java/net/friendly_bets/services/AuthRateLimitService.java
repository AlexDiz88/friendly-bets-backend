package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.config.AppAuthProperties;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AuthRateLimit;
import net.friendly_bets.repositories.AuthRateLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthRateLimitService {

    AuthRateLimitRepository authRateLimitRepository;
    AppAuthProperties appAuthProperties;

    @Transactional
    public void checkAndIncrement(String key) {
        LocalDateTime now = LocalDateTime.now();
        AuthRateLimit limit = authRateLimitRepository.findById(key).orElse(null);

        if (limit == null) {
            authRateLimitRepository.save(AuthRateLimit.builder()
                    .id(key)
                    .attempts(1)
                    .windowStart(now)
                    .build());
            return;
        }

        if (limit.getWindowStart().plusMinutes(appAuthProperties.getRateLimitWindowMinutes()).isBefore(now)) {
            limit.setAttempts(1);
            limit.setWindowStart(now);
            authRateLimitRepository.save(limit);
            return;
        }

        if (limit.getAttempts() >= appAuthProperties.getRateLimitMaxAttempts()) {
            throw new BadRequestException("tooManyAuthRequests");
        }

        limit.setAttempts(limit.getAttempts() + 1);
        authRateLimitRepository.save(limit);
    }
}
