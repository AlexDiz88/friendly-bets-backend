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

    /** Вложенные подгруппы (напр. «П1 + Тотал» внутри «Результат + Тотал»). */
    @Field(name = "subgroups")
    @Builder.Default
    private List<OddsMarketGroup> subgroups = new ArrayList<>();
}
