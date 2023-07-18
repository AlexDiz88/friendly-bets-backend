package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лиги сезона")
public class LeagueDto {

    @Schema(description = "идентификатор лиги", example = "12-битовый хэш ID")
    private String id;

    @Schema(description = "название лиги (автогенерация)", example = "АПЛ-2223")
    private String name;

    @Schema(description = "отображаемое на сайте название лиги (русский)", example = "Бундеслига")
    private String displayNameRu;

    @Schema(description = "отображаемое на сайте название лиги (english)", example = "Bundesliga")
    private String displayNameEn;

    @Schema(description = "список команд лиги", example = "[Team1, Team2...]")
    private List<Team> teams;


    public static LeagueDto from(League league) {
        return LeagueDto.builder()
                .id(league.getId())
                .name(league.getName())
                .displayNameRu(league.getDisplayNameRu())
                .displayNameEn(league.getDisplayNameEn())
                .teams(league.getTeams())
                .build();
    }

    public static List<LeagueDto> from(List<League> leagues) {
        return leagues.stream()
                .map(LeagueDto::from)
                .collect(Collectors.toList());
    }
}
