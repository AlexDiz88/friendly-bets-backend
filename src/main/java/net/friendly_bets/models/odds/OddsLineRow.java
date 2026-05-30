package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OddsLineRow {

    @Field(name = "line")
    private String line;

    @Field(name = "selection_code")
    private String selectionCode;

    @Field(name = "display_label")
    private String displayLabel;

    @Field(name = "bookmaker_odds")
    @Builder.Default
    private Map<String, String> bookmakerOdds = new LinkedHashMap<>();

    @Field(name = "selection_key")
    private String selectionKey;

    @Field(name = "best_odds")
    private String bestOdds;

    @Field(name = "best_bookmaker")
    private String bestBookmaker;
}
