package net.friendly_bets.soccer365.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Soccer365Properties.class)
public class Soccer365Config {
}
