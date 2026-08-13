package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Team;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Команда в топ-метрике игрока")
public class HighlightTeamDto {

    private String id;
    private String title;
    private TeamDisplayNamesDto displayNames;
    private Double actualBalance;

    public static HighlightTeamDto from(Team team, Double actualBalance) {
        if (team == null) {
            return null;
        }
        return HighlightTeamDto.builder()
                .id(team.getId())
                .title(team.getTitle())
                .displayNames(TeamDisplayNamesDto.from(team.getDisplayNames()))
                .actualBalance(actualBalance)
                .build();
    }
}
