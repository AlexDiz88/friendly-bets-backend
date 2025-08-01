package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetTitleCategoryStats;
import net.friendly_bets.models.BetTitleSubcategoryStats;
import net.friendly_bets.models.PlayerStatsByBetTitles;
import net.friendly_bets.models.enums.BetTitleCategory;
import net.friendly_bets.models.enums.BetTitleSubCategory;
import net.friendly_bets.repositories.PlayerStatsByBetTitlesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.friendly_bets.models.enums.BetTitleCategory.CATEGORY_SUBCATEGORY_MAP;
import static net.friendly_bets.utils.StatsUtils.*;


@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetTitleStatsService {

    PlayerStatsByBetTitlesRepository playerStatsByBetTitlesRepository;

    // ------------------------------------------------------------------------------------------------------ //

    public void calculateStatsByBetTitle(String seasonId, String userId, Bet bet, boolean isPlus) {
        short betTitleCode = bet.getBetTitle().getCode();
        BetTitleCategory category = defineBetTitleCategory(betTitleCode);
        BetTitleSubCategory subCategory = defineBetTitleSubCategory(betTitleCode);
        PlayerStatsByBetTitles playerStatsByBetTitles = getStatsByBetTitleOrCreateNew(seasonId, userId);

        BetTitleCategoryStats categoryStats = playerStatsByBetTitles.getBetTitleCategoryStats().stream()
                .filter(cat -> cat.getCategory() == category)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Category not found: " + category));

        BetTitleSubcategoryStats subcategoryStats = categoryStats.getStats().stream()
                .filter(sub -> sub.getSubCategory() == subCategory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Subcategory not found: " + subCategory));

        BetTitleSubcategoryStats subcategorySummaryStats = categoryStats.getStats().stream()
                .filter(sub -> sub.getSubCategory() == BetTitleSubCategory.SUMMARY)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Subcategory not found: " + subCategory));

        modifyBetTitleStats(subcategoryStats, bet, isPlus);
        modifyBetTitleStats(subcategorySummaryStats, bet, isPlus);
        saveStatsByBetTitle(playerStatsByBetTitles);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void modifyBetTitleStats(BetTitleSubcategoryStats stats, Bet bet, boolean isPlus) {
        updateBetCount(stats, isPlus);
        updateBetCountValuesBasedOnBetStatus(stats, bet.getBetStatus(), bet.getBetOdds(), isPlus);
        updateSumOfOddsAndActualBalance(stats, bet.getBetStatus(), bet.getBetOdds(), bet.getBalanceChange(), isPlus);
        recalculateStats(stats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    private void saveStatsByBetTitle(PlayerStatsByBetTitles stats) {
        playerStatsByBetTitlesRepository.save(stats);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStatsByBetTitles getStatsByBetTitleOrCreateNew(String seasonId, String userId) {
        return playerStatsByBetTitlesRepository
                .findBySeasonIdAndUserId(seasonId, userId)
                .orElseGet(() -> createNewStatsByBetTitle(seasonId, userId));
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayerStatsByBetTitles createNewStatsByBetTitle(String seasonId, String userId) {
        List<BetTitleCategoryStats> categoryStatsList = new ArrayList<>();

        for (Map.Entry<BetTitleCategory, Set<BetTitleSubCategory>> entry : CATEGORY_SUBCATEGORY_MAP.entrySet()) {
            BetTitleCategory category = entry.getKey();
            Set<BetTitleSubCategory> subCategories = entry.getValue();

            List<BetTitleSubcategoryStats> subCategoryStatsList = subCategories.stream()
                    .map(this::createNewBetTitleSubcategoryStats)
                    .collect(Collectors.toList());

            BetTitleCategoryStats categoryStats = BetTitleCategoryStats.builder()
                    .category(category)
                    .stats(subCategoryStatsList)
                    .build();

            categoryStatsList.add(categoryStats);
        }

        return PlayerStatsByBetTitles.builder()
                .seasonId(seasonId)
                .userId(userId)
                .betTitleCategoryStats(categoryStatsList)
                .build();
    }

    private BetTitleSubcategoryStats createNewBetTitleSubcategoryStats(BetTitleSubCategory subCategory) {
        return BetTitleSubcategoryStats.builder()
                .subCategory(subCategory)
                .betCount(0)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .emptyBetCount(0)
                .winRate(0.0)
                .averageOdds(0.0)
                .averageWonBetOdds(0.0)
                .actualBalance(0.0)
                .sumOfOdds(0.0)
                .sumOfWonOdds(0.0)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    public BetTitleCategory defineBetTitleCategory(short code) {
        if (code >= 101 && code <= 200) return BetTitleCategory.GAME_RESULTS;
        if (code >= 201 && code <= 800) return BetTitleCategory.GAME_RESULT_AND_TOTALS;
        if (code >= 801 && code <= 1100) return BetTitleCategory.TOTALS;
        if (code >= 1101 && code <= 1200) return BetTitleCategory.HANDICAPS;
        if (code >= 1201 && code <= 1300) return BetTitleCategory.SCORES;
        if (code >= 1301 && code <= 1600) return BetTitleCategory.GOALS;
        if (code >= 2001 && code <= 3700) return BetTitleCategory.HALFTIMES;
        if (code >= 5001 && code <= 5300) return BetTitleCategory.SPECIALS;

        throw new IllegalArgumentException("BetTitle Category can not be defined by code: " + code);
    }

    public BetTitleSubCategory defineBetTitleSubCategory(short code) {
        //========= GAME_RESULTS =========
        if (code == 101) return BetTitleSubCategory.HOME_WIN;
        if (code == 102) return BetTitleSubCategory.DRAW;
        if (code == 103) return BetTitleSubCategory.AWAY_WIN;
        if (code >= 104 && code <= 106) return BetTitleSubCategory.OTHER_RESULTS;

        //========= GAME_RESULT_AND_TOTALS =========
        if (code >= 201 && code <= 250) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;
        if (code >= 301 && code <= 350) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;
        if (code >= 401 && code <= 450) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;
        if (code >= 501 && code <= 550) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;
        if (code >= 601 && code <= 650) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;
        if (code >= 701 && code <= 750) return BetTitleSubCategory.GAME_RESULT_TOTAL_UNDER;

        if (code >= 251 && code <= 300) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;
        if (code >= 351 && code <= 400) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;
        if (code >= 451 && code <= 500) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;
        if (code >= 551 && code <= 600) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;
        if (code >= 651 && code <= 700) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;
        if (code >= 751 && code <= 800) return BetTitleSubCategory.GAME_RESULT_TOTAL_OVER;

        //========= TOTALS =========
        if (code >= 801 && code <= 850) return BetTitleSubCategory.TOTAL_UNDER;
        if (code >= 851 && code <= 900) return BetTitleSubCategory.TOTAL_OVER;
        if (code >= 901 && code <= 1100) return BetTitleSubCategory.PERSONAL_TOTAL;

        //========= HANDICAPS =========
        if (code >= 1101 && code <= 1150) return BetTitleSubCategory.HANDICAP_HOME;
        if (code >= 1151 && code <= 1200) return BetTitleSubCategory.HANDICAP_AWAY;

        //========= SCORES =========
        if (code >= 1201 && code <= 1250) return BetTitleSubCategory.SCORE_0_0_TO_3_3;
        if (code >= 1251 && code <= 1300) return BetTitleSubCategory.SCORE_OTHER;

        //========= GOALS =========
        if (code >= 1301 && code <= 1350) return BetTitleSubCategory.BOTH_SCORES;
        if (code >= 1401 && code <= 1450) return BetTitleSubCategory.BOTH_SCORES_AND_GAME_RESULT;
        if (code >= 1451 && code <= 1500) return BetTitleSubCategory.OTHER;
        if (code >= 1501 && code <= 1600) return BetTitleSubCategory.BOTH_SCORES_AND_TOTAL_GOALS;

        //========= HALFTIMES =========
        if (code >= 1351 && code <= 1400) return BetTitleSubCategory.GOALS_HALFTIME;
        if (code >= 2001 && code <= 2100) return BetTitleSubCategory.GAME_RESULT;
        if (code >= 2101 && code <= 2200) return BetTitleSubCategory.HALF_FULL_FIRST_SECOND;
        if (code >= 2201 && code <= 2300) return BetTitleSubCategory.GAME_SCORE;
        if (code >= 2301 && code <= 2400) return BetTitleSubCategory.COMBO_CONDITION;
        if (code >= 2401 && code <= 2600) return BetTitleSubCategory.HANDICAP;
        if (code >= 2601 && code <= 3200) return BetTitleSubCategory.TOTAL;
        if (code >= 3201 && code <= 3600) return BetTitleSubCategory.COMBO_CONDITION;
        if (code >= 3601 && code <= 3700) return BetTitleSubCategory.GOALS_HALFTIME;

        //========= SPECIALS =========
        if (code >= 5001 && code <= 5100) return BetTitleSubCategory.CLEAN_WIN;
        if (code >= 5101 && code <= 5200) return BetTitleSubCategory.GOALS_DIFFERENCE;
        if (code >= 5201 && code <= 5300) return BetTitleSubCategory.PLAYOFF;

        throw new IllegalArgumentException("BetTitle SubCategory can not be defined by code: " + code);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
