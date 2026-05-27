package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.config.AppAuthProperties;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AccountToken;
import net.friendly_bets.models.AccountTokenType;
import net.friendly_bets.repositories.AccountTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountTokenService {

    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    AccountTokenRepository accountTokenRepository;
    AppAuthProperties appAuthProperties;

    @Transactional
    public String createToken(String userId, AccountTokenType type) {
        accountTokenRepository.deleteByUserIdAndTypeAndUsedAtIsNull(userId, type);

        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(expiryHours(type));

        AccountToken token = AccountToken.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .type(type)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
        accountTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public AccountToken consumeToken(String rawToken, AccountTokenType type) {
        AccountToken token = accountTokenRepository
                .findByTokenHashAndTypeAndUsedAtIsNull(hashToken(rawToken), type)
                .orElseThrow(() -> new BadRequestException("invalidOrExpiredToken"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("invalidOrExpiredToken");
        }

        token.setUsedAt(LocalDateTime.now());
        accountTokenRepository.save(token);
        return token;
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = rawToken + appAuthProperties.getTokenPepper();
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private int expiryHours(AccountTokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> appAuthProperties.getEmailVerificationExpiryHours();
            case PASSWORD_RESET -> appAuthProperties.getPasswordResetExpiryHours();
        };
    }
}
