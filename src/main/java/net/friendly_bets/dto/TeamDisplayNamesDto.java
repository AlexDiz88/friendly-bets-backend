package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.TeamDisplayNames;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TeamDisplayNamesDto {

    private String en;
    private String ru;
    private String de;

    public static TeamDisplayNamesDto from(TeamDisplayNames names) {
        if (names == null) {
            return null;
        }
        return TeamDisplayNamesDto.builder()
                .en(names.getEn())
                .ru(names.getRu())
                .de(names.getDe())
                .build();
    }

    public TeamDisplayNames toEntity() {
        return TeamDisplayNames.builder()
                .en(en)
                .ru(ru)
                .de(de)
                .build();
    }
}
