package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
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
        removeExistingResultTotal(groups);
        List<OddsMarketGroup> calculated = OddsResultTotalCalculator.buildGroups(groups, bookmakers);
        if (calculated.isEmpty()) {
            return;
        }
        groups.addAll(calculated);
        sortGroups(groups);
        OddsSelectionKey.enrichGroups(calculated);
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
            try {
                OddsMarketCategory category = OddsMarketCategory.valueOf(group.getCategory());
                group.setSortOrder(category.getSortOrder());
                group.setCollapsedByDefault(category.isCollapsedByDefault());
                group.setGroupKey(OddsMarketCatalog.i18nGroupKey(category));
            } catch (IllegalArgumentException ignored) {
                // legacy category names — leave as stored
            }
        }
        sortGroups(groups);
    }

    private static void removeExistingResultTotal(List<OddsMarketGroup> groups) {
        groups.removeIf(g -> g != null && (
                "RESULT_TOTAL".equals(g.getCategory())
                        || "RESULT_TOTAL_OVER".equals(g.getCategory())
                        || "RESULT_TOTAL_UNDER".equals(g.getCategory())));
    }
}
