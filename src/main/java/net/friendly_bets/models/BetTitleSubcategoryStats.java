package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.friendly_bets.models.enums.BetTitleSubCategory;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = false)
public class BetTitleSubcategoryStats extends Stats {

    private BetTitleSubCategory subCategory;
}
