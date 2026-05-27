package net.friendly_bets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    private String frontendBaseUrl = "http://localhost:5173";
    private String tokenPepper = "";
    private int emailVerificationExpiryHours = 48;
    private int passwordResetExpiryHours = 1;
    private int rateLimitMaxAttempts = 5;
    private int rateLimitWindowMinutes = 60;

    /** When true, users with email_is_confirmed=false cannot sign in. */
    private boolean requireEmailConfirmedForLogin = false;
}
