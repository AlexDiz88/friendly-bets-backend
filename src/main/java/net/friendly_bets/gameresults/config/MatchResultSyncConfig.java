package net.friendly_bets.gameresults.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchResultSyncProperties.class)
public class MatchResultSyncConfig {
}
