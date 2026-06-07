package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsResultTotalSubgroupSplitter;
import net.friendly_bets.oddsapi.OddsSelectionKey;

import java.util.Comparator;
import java.util.List;

/**
 * Добавляет рассчитанные группы «Результат + ТБ» и «Результат + ТМ» к смерженным рынкам.
 */
public final class OddsResultTotalEnricher {

    private OddsResultTotalEnricher() {
    }

    /**
     * Пересчитывает «Результат + ТБ/ТМ» (нужно при чтении из Mongo — в БД могут быть устаревшие группы).
     */
    public static void appendCalculatedGroups(List<OddsMarketGroup> groups, List<String> bookmakers) {
        if (groups == null) {
            return;
        }
        if (hasMarathonbetResultTotal(groups)) {
            OddsResultTotalSubgroupSplitter.splitIntoSubgroups(groups);
            OddsSelectionKey.enrichGroups(groups);
            return;
        }
        removeExistingResultTotal(groups);
        List<OddsMarketGroup> calculated = OddsResultTotalCalculator.buildGroups(groups, bookmakers);
        if (calculated.isEmpty()) {
            return;
        }
        groups.addAll(calculated);
        OddsResultTotalSubgroupSplitter.splitIntoSubgroups(groups);
        sortGroups(groups);
        OddsSelectionKey.enrichGroups(groups);
    }

    private static boolean hasMarathonbetResultTotal(List<OddsMarketGroup> groups) {
        if (groups == null) {
            return false;
        }
        for (OddsMarketGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (group.getRows() != null && isResultTotalCategoryName(group.getCategory())) {
                for (OddsLineRow row : group.getRows()) {
                    if (row.getBookmakerOdds() != null
                            && row.getBookmakerOdds().containsKey(MarathonbetBookmaker.KEY)) {
                        return true;
                    }
                }
            }
            if (group.getSubgroups() != null && hasMarathonbetResultTotal(group.getSubgroups())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isResultTotalCategoryName(String category) {
        return OddsMarketCategory.RESULT_TOTAL_OVER.name().equals(category)
                || OddsMarketCategory.RESULT_TOTAL_UNDER.name().equals(category)
                || "RESULT_TOTAL".equals(category);
    }

    public static void sortGroups(List<OddsMarketGroup> groups) {
        if (groups != null) {
            groups.sort(Comparator.comparingInt(OddsMarketGroup::getSortOrder));
        }
    }

    /** sortOrder / collapsed / groupKey из enum (актуально даже для старых документов Mongo). */
    public static void applyCategoryMetadata(List<OddsMarketGroup> groups) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            if (group.getCategory() == null) {
                continue;
            }
            applyMetadataToGroup(group);
            if (group.getSubgroups() != null) {
                for (OddsMarketGroup sub : group.getSubgroups()) {
                    applyMetadataToGroup(sub);
                }
            }
        }
        sortGroups(groups);
    }

    private static void applyMetadataToGroup(OddsMarketGroup group) {
        if (group.getCategory() == null) {
            return;
        }
        if ("RESULT_TOTAL".equals(group.getCategory())) {
            group.setSortOrder(OddsMarketCategory.RESULT_TOTAL_OVER.getSortOrder());
            group.setCollapsedByDefault(true);
            if (group.getGroupKey() == null || group.getGroupKey().equals(group.getCategory())) {
                group.setGroupKey("resultTotal");
            }
            return;
        }
        if ("HALF_CORRECT_SCORE".equals(group.getCategory())) {
            group.setSortOrder(OddsMarketCategory.FIRST_HALF_CORRECT_SCORE.getSortOrder());
            group.setCollapsedByDefault(true);
            if (group.getGroupKey() == null || group.getGroupKey().equals(group.getCategory())) {
                group.setGroupKey("halfCorrectScore");
            }
            return;
        }
        try {
            OddsMarketCategory category = OddsMarketCategory.valueOf(group.getCategory());
            group.setSortOrder(category.getSortOrder());
            group.setCollapsedByDefault(category.isCollapsedByDefault());
            String defaultKey = OddsMarketCatalog.i18nGroupKey(category);
            if (group.getGroupKey() == null
                    || group.getGroupKey().equals(group.getCategory())
                    || group.getGroupKey().equals(defaultKey)) {
                group.setGroupKey(defaultKey);
            }
        } catch (IllegalArgumentException ignored) {
            // legacy category names — leave as stored
        }
    }

    private static void removeExistingResultTotal(List<OddsMarketGroup> groups) {
        groups.removeIf(g -> g != null && isResultTotalTopLevel(g));
    }

    private static boolean isResultTotalTopLevel(OddsMarketGroup g) {
        if ("RESULT_TOTAL".equals(g.getCategory())) {
            return true;
        }
        if ("RESULT_TOTAL_OVER".equals(g.getCategory()) || "RESULT_TOTAL_UNDER".equals(g.getCategory())) {
            return true;
        }
        String key = g.getGroupKey();
        if (key == null) {
            return false;
        }
        return key.equals("resultTotal")
                || key.equals("resultTotalOver")
                || key.equals("resultTotalUnder")
                || key.startsWith("resultTotal");
    }
}
