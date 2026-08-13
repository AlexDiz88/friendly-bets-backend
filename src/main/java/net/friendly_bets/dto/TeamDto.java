package net.friendly_bets.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Team;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Команда")
public class TeamDto {

    @Schema(description = "идентификатор команды", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "ключ команды (PascalCase)", example = "ManchesterUnited")
    private String title;

    private String country;
    private String logoKey;
    private TeamDisplayNamesDto displayNames;
    private List<TeamExternalAliasDto> externalAliases;

    public static TeamDto from(Team team) {
        if (team == null) {
            return null;
        }
        List<TeamExternalAliasDto> aliases = team.getExternalAliases() == null
                ? Collections.emptyList()
                : team.getExternalAliases().stream().map(TeamExternalAliasDto::from).toList();
        return TeamDto.builder()
                .id(team.getId())
                .title(team.getTitle())
                .country(team.getCountry())
                .logoKey(team.getLogo())
                .displayNames(TeamDisplayNamesDto.from(team.getDisplayNames()))
                .externalAliases(aliases)
                .build();
    }

    /** Карточка ставки: без externalAliases (это данные синка, не UI). */
    public static TeamDto fromForBet(Team team) {
        if (team == null) {
            return null;
        }
        return TeamDto.builder()
                .id(team.getId())
                .title(team.getTitle())
                .country(team.getCountry())
                .logoKey(team.getLogo())
                .displayNames(TeamDisplayNamesDto.from(team.getDisplayNames()))
                .build();
    }

    public static List<TeamDto> from(List<Team> teams) {
        return teams.stream()
                .map(TeamDto::from)
                .collect(Collectors.toList());
    }
}
