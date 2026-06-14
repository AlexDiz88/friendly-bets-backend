package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26FifaBracketPageDto {

    private List<Wc26FifaBracketMatchDto> matches;
    private LocalDateTime fetchedAt;
    private String sourceUrl;
}
