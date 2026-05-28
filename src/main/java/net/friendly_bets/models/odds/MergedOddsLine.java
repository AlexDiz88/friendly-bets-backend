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
public class MergedOddsLine {

    @Field(name = "market_name")
    private String marketName;

    @Field(name = "line")
    private String line;

    @Field(name = "selections")
    @Builder.Default
    private List<MergedOddsSelection> selections = new ArrayList<>();
}
