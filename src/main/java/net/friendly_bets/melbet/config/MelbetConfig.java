package net.friendly_bets.melbet.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MelbetProperties.class)
public class MelbetConfig {
}
