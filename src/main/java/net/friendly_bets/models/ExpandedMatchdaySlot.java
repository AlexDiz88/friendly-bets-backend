package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExpandedMatchdaySlot {

    public enum Kind {
        REGULAR, GROUP, KNOCKOUT
    }

    /** Канонический id = Bet.match_day */
    private String id;

    private int order;

    private Kind kind;

    /** Ключ i18n playoffStage.* или число тура */
    private String labelKey;
}
