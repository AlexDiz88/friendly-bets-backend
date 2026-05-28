package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCorrectGameResultDto {

    @NotBlank
    private String fullTime;

    private String firstTime;
    private String overTime;
    private String penalty;
}
