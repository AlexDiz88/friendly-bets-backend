package net.friendly_bets.api_football.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballFixtureDto {

    private Fixture fixture;
    private League league;
    private Teams teams;
    private Goals goals;
    private Score score;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private long id;
        private String date;
        private Status status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @com.fasterxml.jackson.annotation.JsonProperty("short")
        private String shortStatus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private int id;
        private int season;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Teams {
        private Side home;
        private Side away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Side {
        private int id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        private Integer home;
        private Integer away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private Part halftime;
        private Part fulltime;
        private Part extratime;
        private Part penalty;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private Integer home;
        private Integer away;
    }
}
