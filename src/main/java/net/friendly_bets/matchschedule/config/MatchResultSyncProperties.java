package net.friendly_bets.matchschedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "match-result-sync")
public class MatchResultSyncProperties {

    /** Автоматически вызывать settle после FULL finalize. */
    private boolean autoSettleEnabled = true;
    /** LIVE poll interval (default 5 minutes). */
    private long pollingIntervalMs = 300_000L;
    private String defaultSeason = "2025";
    /**
     * Id пользователя-модератора для аудита auto-settle. Пусто — первый ADMIN в БД.
     */
    private String systemModeratorId = "";
}
