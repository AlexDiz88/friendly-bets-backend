package net.friendly_bets.twentyfourscore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(TwentyFourScoreProperties.class)
public class TwentyFourScoreConfig {

    @Bean(name = "twentyFourScoreLiveTaskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler twentyFourScoreLiveTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("24score-live-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
