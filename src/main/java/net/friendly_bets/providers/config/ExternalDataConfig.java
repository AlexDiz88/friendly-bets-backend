package net.friendly_bets.providers.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExternalDataProperties.class)
public class ExternalDataConfig {
}
