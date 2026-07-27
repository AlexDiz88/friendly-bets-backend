package net.friendly_bets.matchschedule.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchResultSyncProperties.class)
public class MatchResultSyncConfig {
}
