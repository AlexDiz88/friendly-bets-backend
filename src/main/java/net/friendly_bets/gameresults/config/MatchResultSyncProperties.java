package net.friendly_bets.gameresults.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "match-result-sync")
public class MatchResultSyncProperties {

    private String primaryProvider = "4score.ru";
    private String secondaryProvider = "24score.pro";
    private boolean dualVerificationEnabled = true;
    private boolean allowFinalizeWithoutSecondary = false;
    private int requireStablePolls = 2;
    /** @deprecated не используется в isStableEnough; оставлено для обратной совместимости админки */
    private int minMinutesAfterKickoff = 0;
    /** @deprecated не используется в isStableEnough */
    private int minMinutesAfterKickoffKnockout = 0;
    /** @deprecated не используется в isStableEnough */
    private int minMinutesSinceApiLastUpdated = 0;
    private boolean autoSettleOnlyWhenMatchdayCompleted = false;
    /** Автоматически вызывать {@code setBetResults} после опроса провайдеров. */
    private boolean autoSettleEnabled = true;
    /** 15 minutes */
    private long pollingIntervalMs = 900_000L;
    private String defaultSeason = "2025";
    /**
     * Id пользователя-модератора для аудита auto-settle. Пусто — первый ADMIN в БД.
     */
    private String systemModeratorId = "";
}
