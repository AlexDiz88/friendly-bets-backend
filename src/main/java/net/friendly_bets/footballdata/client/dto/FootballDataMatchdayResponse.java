package net.friendly_bets.footballdata.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataMatchdayResponse {

    private Filters filters;
    private ResultSet resultSet;
    private Competition competition;
    private List<FootballDataMatchDto> matches;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filters {
        private String season;
        private String matchday;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultSet {
        private int count;
        private String first;
        private String last;
        private int played;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        private String code;
    }
}
