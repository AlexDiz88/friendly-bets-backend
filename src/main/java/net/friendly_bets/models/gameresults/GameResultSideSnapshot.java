package net.friendly_bets.models.gameresults;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GameResultSideSnapshot {

    @Field(name = "external_id")
    private String externalId;

    @Field(name = "external_name")
    private String externalName;
}
