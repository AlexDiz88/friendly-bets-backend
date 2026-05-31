package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RoundRobinStage {

    @Field(name = "matchday_count")
    private int matchdayCount;

    /**
     * Групповой этап: каждый тур делится на N слотов {@code 1 [1]} …
     */
    @Field(name = "split_slots_per_round")
    private Boolean splitSlotsPerRound;

    @Field(name = "slots_per_round")
    private List<Integer> slotsPerRound;

    public boolean isSplitSlotsPerRound() {
        return Boolean.TRUE.equals(splitSlotsPerRound);
    }
}
