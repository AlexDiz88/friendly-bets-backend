package net.friendly_bets.footballdata.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataMatchDto {

    private long id;
    private String utcDate;
    private String status;
    private int matchday;
    private String stage;
    private String lastUpdated;
    private Team homeTeam;
    private Team awayTeam;
    private Score score;
    private Competition competition;
    private Season season;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private int id;
        private String name;
        private String shortName;
        private String tla;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private ScoreLine fullTime;
        private ScoreLine halfTime;
        private ScoreLine extraTime;
        private ScoreLine penalties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreLine {
        private Integer home;
        private Integer away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        private String code;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Season {
        private String startDate;
    }
}
