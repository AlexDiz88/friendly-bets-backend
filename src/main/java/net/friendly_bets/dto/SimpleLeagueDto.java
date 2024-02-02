package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лига сезона (без списка команд)")
public class SimpleLeagueDto {

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "название лиги (автогенерация)", example = "АПЛ-2223")
    private String name;

    @Schema(description = "отображаемое на сайте название лиги (русский)", example = "Бундеслига")
    private String displayNameRu;

    @Schema(description = "отображаемое на сайте название лиги (english)", example = "Bundesliga")
    private String displayNameEn;

    @Schema(description = "сокарщенное имя лиги (русский)", example = "БЛ")
    private String shortNameRu;

    @Schema(description = "сокарщенное имя лиги (english)", example = "BL")
    private String shortNameEn;

    @Schema(description = "текущий игровой тур лиги", example = "14")
    private String currentMatchDay;

    public static SimpleLeagueDto from(League league) {
        return SimpleLeagueDto.builder()
                .id(league.getId())
                .name(league.getName())
                .displayNameRu(league.getDisplayNameRu())
                .displayNameEn(league.getDisplayNameEn())
                .shortNameRu(league.getShortNameRu())
                .shortNameEn(league.getShortNameEn())
                .currentMatchDay(league.getCurrentMatchDay())
                .build();
    }

    public static List<SimpleLeagueDto> from(List<League> leagues) {
        return leagues.stream()
                .map(SimpleLeagueDto::from)
                .collect(Collectors.toList());
    }
}
