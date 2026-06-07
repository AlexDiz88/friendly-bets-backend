package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Группирует точный счёт 1-го и 2-го тайма в родительскую категорию с подгруппами.
 */
public final class OddsHalfCorrectScoreSubgroupSplitter {

    private record SubgroupDef(
            String groupKey,
            int codeMin,
            int codeMax,
            OddsMarketCategory category
    ) {
    }

    private static final List<SubgroupDef> ORDERED_SUBGROUPS = List.of(
            new SubgroupDef("firstHalfCorrectScore", 2201, 2250, OddsMarketCategory.FIRST_HALF_CORRECT_SCORE),
            new SubgroupDef("secondHalfCorrectScore", 2251, 2300, OddsMarketCategory.SECOND_HALF_CORRECT_SCORE)
    );

    private OddsHalfCorrectScoreSubgroupSplitter() {
    }

    public static void splitIntoSubgroups(List<OddsMarketGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        List<OddsLineRow> firstHalfRows = collectRows(groups, OddsMarketCategory.FIRST_HALF_CORRECT_SCORE);
        List<OddsLineRow> secondHalfRows = collectRows(groups, OddsMarketCategory.SECOND_HALF_CORRECT_SCORE);
        if (firstHalfRows.isEmpty() && secondHalfRows.isEmpty()) {
            return;
        }
        groups.removeIf(OddsHalfCorrectScoreSubgroupSplitter::isHalfCorrectScoreGroup);

        List<OddsMarketGroup> subgroups = new ArrayList<>();
        int idx = 0;
        int baseSort = OddsMarketCategory.FIRST_HALF_CORRECT_SCORE.getSortOrder();
        for (SubgroupDef def : ORDERED_SUBGROUPS) {
            List<OddsLineRow> source = def.category() == OddsMarketCategory.FIRST_HALF_CORRECT_SCORE
                    ? firstHalfRows
                    : secondHalfRows;
            OddsMarketGroup child = buildSubgroup(def, source, baseSort, idx++);
            if (child != null) {
                subgroups.add(child);
            }
        }
        if (subgroups.isEmpty()) {
            return;
        }
        groups.add(OddsMarketGroup.builder()
                .category("HALF_CORRECT_SCORE")
                .groupKey("halfCorrectScore")
                .sortOrder(baseSort)
                .collapsedByDefault(true)
                .rows(new ArrayList<>())
                .subgroups(subgroups)
                .build());
    }

    private static OddsMarketGroup buildSubgroup(
            SubgroupDef def,
            List<OddsLineRow> source,
            int baseSort,
            int index
    ) {
        List<OddsLineRow> rows = source.stream()
                .filter(row -> codeInRange(row, def.codeMin(), def.codeMax()))
                .sorted(correctScoreComparator())
                .toList();
        if (rows.isEmpty()) {
            return null;
        }
        return OddsMarketGroup.builder()
                .category(def.category().name())
                .groupKey(def.groupKey())
                .sortOrder(baseSort * 100 + index)
                .collapsedByDefault(true)
                .rows(new ArrayList<>(rows))
                .build();
    }

    private static Comparator<OddsLineRow> correctScoreComparator() {
        return Comparator.comparingInt(row -> {
            String selection = row.getSelectionCode();
            if (selection != null && !selection.isBlank()) {
                return OddsCorrectScoreUtils.sortKey(selection);
            }
            if (row.getBetTitle() != null) {
                String derived = OddsCorrectScoreUtils.selectionCodeForBetTitle(
                        net.friendly_bets.models.enums.BetTitleCode.fromCode(row.getBetTitle().getCode()));
                if (derived != null) {
                    return OddsCorrectScoreUtils.sortKey(derived);
                }
            }
            return Integer.MAX_VALUE;
        });
    }

    private static boolean isHalfCorrectScoreGroup(OddsMarketGroup group) {
        if (group == null) {
            return false;
        }
        if (isHalfCorrectScoreCategory(group.getCategory())) {
            return true;
        }
        String key = group.getGroupKey();
        if (key == null) {
            return false;
        }
        return "halfCorrectScore".equals(key)
                || key.startsWith("firstHalfCorrectScore")
                || key.startsWith("secondHalfCorrectScore");
    }

    private static boolean isHalfCorrectScoreCategory(String category) {
        return OddsMarketCategory.FIRST_HALF_CORRECT_SCORE.name().equals(category)
                || OddsMarketCategory.SECOND_HALF_CORRECT_SCORE.name().equals(category)
                || "HALF_CORRECT_SCORE".equals(category);
    }

    private static boolean codeInRange(OddsLineRow row, int min, int max) {
        BetTitle betTitle = row.getBetTitle();
        if (betTitle == null) {
            return false;
        }
        short code = betTitle.getCode();
        return code >= min && code <= max;
    }

    private static List<OddsLineRow> collectRows(List<OddsMarketGroup> groups, OddsMarketCategory category) {
        List<OddsLineRow> rows = new ArrayList<>();
        collectRowsRecursive(groups, category, rows);
        return rows;
    }

    private static void collectRowsRecursive(
            List<OddsMarketGroup> groups,
            OddsMarketCategory category,
            List<OddsLineRow> rows
    ) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (category.name().equals(group.getCategory()) && group.getRows() != null) {
                rows.addAll(group.getRows());
            }
            if (group.getSubgroups() != null) {
                collectRowsRecursive(group.getSubgroups(), category, rows);
            }
        }
    }
}
