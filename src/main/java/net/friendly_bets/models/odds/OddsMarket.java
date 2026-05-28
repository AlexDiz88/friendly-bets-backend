package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OddsMarket {

    @Field(name = "name")
    private String name;

    @Field(name = "outcomes")
    @Builder.Default
    private List<OddsOutcome> outcomes = new ArrayList<>();
}
