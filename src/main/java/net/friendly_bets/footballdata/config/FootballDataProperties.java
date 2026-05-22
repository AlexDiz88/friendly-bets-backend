package net.friendly_bets.footballdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "football-data")
public class FootballDataProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.football-data.org/v4";
    private boolean syncEnabled = true;
    private long pollingIntervalMs = 900_000L;
    private String defaultSeason = "2025";

    /**
     * football-data team id -> точное title команды в MongoDB.
     */
    private Map<Integer, String> teamIds = new HashMap<>();

    /**
     * Имя команды из API -> title команды в MongoDB (fallback).
     */
    private Map<String, String> teamNames = new HashMap<>();
}
