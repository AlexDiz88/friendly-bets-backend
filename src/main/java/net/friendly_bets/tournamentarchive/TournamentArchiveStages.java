package net.friendly_bets.tournamentarchive;

/**
 * Стадии в архиве: group_1..3, 1/16, 1/8, 1/4, 1/2, third_place, final.
 * UI ЧМ: group_1..3 (туры) и round_of_32 / … для плей-офф-фильтров.
 */
public final class TournamentArchiveStages {

    private TournamentArchiveStages() {
    }

    public static boolean isGroupStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return false;
        }
        return "group".equals(stage) || stage.startsWith("group_");
    }

    public static boolean isKnockout(String stage) {
        return stage != null && !stage.isBlank() && !isGroupStage(stage);
    }

    /**
     * Архив → код для WC26 UI.
     * group_1/2/3 оставляем как есть (фильтры туров); плей-офф — в round_of_*.
     */
    public static String toUiStage(String archiveStage) {
        if (archiveStage == null) {
            return null;
        }
        if ("group".equals(archiveStage)
                || "group_1".equals(archiveStage)
                || "group_2".equals(archiveStage)
                || "group_3".equals(archiveStage)) {
            return archiveStage.equals("group") ? "group" : archiveStage;
        }
        return switch (archiveStage) {
            case "1/16", "round_of_32" -> "round_of_32";
            case "1/8", "round_of_16" -> "round_of_16";
            case "1/4", "quarter_final" -> "quarter_final";
            case "1/2", "semi_final" -> "semi_final";
            case "third_place", "final" -> archiveStage;
            default -> archiveStage;
        };
    }

    /** UI / query filter → канон архива. */
    public static String toArchiveStage(String stageOrFilter) {
        if (stageOrFilter == null) {
            return null;
        }
        return switch (stageOrFilter.trim()) {
            case "round_of_32", "1/16" -> "1/16";
            case "round_of_16", "1/8" -> "1/8";
            case "quarter_final", "1/4" -> "1/4";
            case "semi_final", "1/2" -> "1/2";
            case "group_r1", "group_1" -> "group_1";
            case "group_r2", "group_2" -> "group_2";
            case "group_r3", "group_3" -> "group_3";
            default -> stageOrFilter.trim();
        };
    }

    public static boolean stageMatchesFilter(String archiveStage, String stageFilter) {
        if (stageFilter == null || stageFilter.isBlank() || "all".equalsIgnoreCase(stageFilter)) {
            return true;
        }
        String wanted = toArchiveStage(stageFilter);
        String actual = toArchiveStage(archiveStage);
        return wanted != null && wanted.equals(actual);
    }

    /** Match numbers 1–24 / 25–48 / 49–72 → group_1 / group_2 / group_3. */
    public static String groupStageForMatchNumber(int matchNumber) {
        if (matchNumber >= 1 && matchNumber <= 24) {
            return "group_1";
        }
        if (matchNumber >= 25 && matchNumber <= 48) {
            return "group_2";
        }
        if (matchNumber >= 49 && matchNumber <= 72) {
            return "group_3";
        }
        return "group";
    }
}
