package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewLeagueDto {

    private String displayNameRu;
    private String displayNameEn;
    private String shortNameRu;
    private String shortNameEn;
}
