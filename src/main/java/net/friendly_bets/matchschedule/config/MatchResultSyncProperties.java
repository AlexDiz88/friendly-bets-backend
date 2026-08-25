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

    /** Пауза после LIVE→FINISHED перед первым вызовом FULL (дать источнику дописать карточку). */
    private long fullMatchInitialDelayMs = 300_000L;
    /** Пауза после первой неуспешной попытки FULL (not-ready / not-found / parse / HTTP). */
    private long fullMatchRetryDelayMs = 300_000L;
    /** Пауза после повторной неуспешной попытки FULL (и далее): раз в час. */
    private long fullMatchHourlyDelayMs = 3_600_000L;
}
