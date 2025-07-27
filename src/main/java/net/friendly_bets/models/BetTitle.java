package net.friendly_bets.models;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isNot")
    private boolean isNot;
}
