package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BetTitle {
    private short code;
    private String label;
    private boolean isNot;
}
