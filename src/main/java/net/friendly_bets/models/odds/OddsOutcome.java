package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OddsOutcome {

    @Field(name = "label")
    private String label;

    @Field(name = "odds")
    private String odds;

    @Field(name = "line")
    private String line;
}
