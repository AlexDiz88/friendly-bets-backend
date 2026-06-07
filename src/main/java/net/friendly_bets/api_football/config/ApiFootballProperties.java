package net.friendly_bets.api_football.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "api-football")
public class ApiFootballProperties {

    private String apiKey = "";
    private String baseUrl = "https://v3.football.api-sports.io";
    private boolean syncEnabled = false;
}
