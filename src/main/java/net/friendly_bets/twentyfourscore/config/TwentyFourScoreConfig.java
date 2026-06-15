package net.friendly_bets.twentyfourscore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(TwentyFourScoreProperties.class)
public class TwentyFourScoreConfig {

    @Bean
    public RestTemplate twentyFourScoreRestTemplate(
            TwentyFourScoreProperties properties,
            RestTemplateBuilder builder
    ) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }
}
