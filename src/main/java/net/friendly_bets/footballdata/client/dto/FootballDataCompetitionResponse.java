package net.friendly_bets.footballdata.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataCompetitionResponse {

    private String code;
    private String type;
    private Season currentSeason;
    private List<Season> seasons;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Season {
        private Integer id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer currentMatchday;
        private List<String> stages;
    }
}
