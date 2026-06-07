package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewSeasonDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String title;

    @NotNull(message = "{field.isNull}")
    @Min(value = 1, message = "{betsInSeason.minCount}")
    private Integer betCountPerMatchDay;

    @NotNull(message = "{field.isNull}")
    @Min(value = 1, message = "{field.bet.betSizeMinValue}")
    private Integer defaultBetSize;

    @NotNull(message = "{field.isNull}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "{field.isNull}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
