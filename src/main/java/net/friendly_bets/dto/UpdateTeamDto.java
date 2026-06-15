package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateTeamDto {

    private String country;
    private TeamDisplayNamesDto displayNames;
    private List<TeamExternalAliasDto> externalAliases;
}
