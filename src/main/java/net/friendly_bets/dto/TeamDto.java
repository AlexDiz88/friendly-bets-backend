package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Team;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Команда")
public class TeamDto {

    @Schema(description = "идентификатор команды", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "название команды (english)", example = "Arsenal")
    private String title;

    public static TeamDto from(Team team) {
        return TeamDto.builder()
                .id(team.getId())
                .title(team.getTitle())
                .build();
    }

    public static List<TeamDto> from(List<Team> teams) {
        return teams.stream()
                .map(TeamDto::from)
                .collect(Collectors.toList());
    }
}
