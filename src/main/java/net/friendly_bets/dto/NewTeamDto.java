package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewTeamDto {

    private String fullTitleRu;
    private String fullTitleEn;
    private String country;
}
