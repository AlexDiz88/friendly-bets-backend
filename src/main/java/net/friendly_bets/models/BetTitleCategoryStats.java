package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.enums.BetTitleCategory;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BetTitleCategoryStats {

    private BetTitleCategory category;
    private List<BetTitleSubcategoryStats> stats;
}
