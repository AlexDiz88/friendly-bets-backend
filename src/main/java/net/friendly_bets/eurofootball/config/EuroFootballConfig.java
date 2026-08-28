package net.friendly_bets.eurofootball.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EuroFootballProperties.class)
public class EuroFootballConfig {
}
