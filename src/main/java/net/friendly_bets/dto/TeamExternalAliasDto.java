package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.TeamExternalAlias;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TeamExternalAliasDto {

    private String provider;
    private Integer externalId;
    private String externalName;

    public static TeamExternalAliasDto from(TeamExternalAlias alias) {
        if (alias == null) {
            return null;
        }
        return TeamExternalAliasDto.builder()
                .provider(alias.getProvider())
                .externalId(alias.getExternalId())
                .externalName(alias.getExternalName())
                .build();
    }

    public TeamExternalAlias toEntity() {
        return TeamExternalAlias.builder()
                .provider(provider)
                .externalId(externalId)
                .externalName(externalName)
                .build();
    }
}
