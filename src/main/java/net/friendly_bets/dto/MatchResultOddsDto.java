package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact 1X2 (П1 / X / П2) best odds for matchday card preview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultOddsDto {

    private String home;
    private String draw;
    private String away;
}
