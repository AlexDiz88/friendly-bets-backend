package net.friendly_bets.footballdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "football-data")
public class FootballDataProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.football-data.org/v4";
    private boolean syncEnabled = true;
    /** Автоматически вызывать {@code setBetResults} после опроса API. */
    private boolean autoSettleEnabled = true;
    private long pollingIntervalMs = 900_000L;
    private String defaultSeason = "2025";
    /**
     * Id пользователя-модератора для аудита auto-settle. Пусто — первый ADMIN в БД.
     */
    private String systemModeratorId = "";
}
