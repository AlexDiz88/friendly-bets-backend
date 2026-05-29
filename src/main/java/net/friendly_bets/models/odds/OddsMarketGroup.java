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
public class OddsMarketGroup {

    @Field(name = "category")
    private String category;

    @Field(name = "group_key")
    private String groupKey;

    @Field(name = "sort_order")
    private int sortOrder;

    @Field(name = "collapsed_by_default")
    private boolean collapsedByDefault;

    @Field(name = "rows")
    @Builder.Default
    private List<OddsLineRow> rows = new ArrayList<>();
}
