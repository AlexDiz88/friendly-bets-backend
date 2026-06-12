package net.friendly_bets.wc26;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Group-stage schedule ids 1–72 — mirrors frontend {@code wc26Schedule.ts}.
 */
public final class Wc26ScheduleCatalog {

    public record GroupMatch(int scheduleId, String homeFifa, String awayFifa) {
    }

    private static final Map<Integer, GroupMatch> STATIC_BY_ID = new LinkedHashMap<>();
    private static volatile Map<Integer, GroupMatch> dbById = Map.of();

    static {
        gm(1, "MEX", "RSA");
        gm(2, "KOR", "CZE");
        gm(3, "CAN", "BIH");
        gm(4, "USA", "PAR");
        gm(5, "HAI", "SCO");
        gm(6, "AUS", "TUR");
        gm(7, "BRA", "MAR");
        gm(8, "QAT", "SUI");
        gm(9, "CIV", "ECU");
        gm(10, "GER", "CUW");
        gm(11, "NED", "JPN");
        gm(12, "SWE", "TUN");
        gm(13, "KSA", "URU");
        gm(14, "ESP", "CPV");
        gm(15, "IRN", "NZL");
        gm(16, "BEL", "EGY");
        gm(17, "FRA", "SEN");
        gm(18, "IRQ", "NOR");
        gm(19, "ARG", "ALG");
        gm(20, "AUT", "JOR");
        gm(21, "GHA", "PAN");
        gm(22, "ENG", "CRO");
        gm(23, "POR", "COD");
        gm(24, "UZB", "COL");
        gm(25, "CZE", "RSA");
        gm(26, "SUI", "BIH");
        gm(27, "CAN", "QAT");
        gm(28, "MEX", "KOR");
        gm(29, "BRA", "HAI");
        gm(30, "SCO", "MAR");
        gm(31, "TUR", "PAR");
        gm(32, "USA", "AUS");
        gm(33, "GER", "CIV");
        gm(34, "ECU", "CUW");
        gm(35, "NED", "SWE");
        gm(36, "TUN", "JPN");
        gm(37, "URU", "CPV");
        gm(38, "ESP", "KSA");
        gm(39, "BEL", "IRN");
        gm(40, "NZL", "EGY");
        gm(41, "NOR", "SEN");
        gm(42, "FRA", "IRQ");
        gm(43, "ARG", "AUT");
        gm(44, "JOR", "ALG");
        gm(45, "ENG", "GHA");
        gm(46, "PAN", "CRO");
        gm(47, "POR", "UZB");
        gm(48, "COL", "COD");
        gm(49, "SCO", "BRA");
        gm(50, "MAR", "HAI");
        gm(51, "SUI", "CAN");
        gm(52, "BIH", "QAT");
        gm(53, "CZE", "MEX");
        gm(54, "RSA", "KOR");
        gm(55, "CUW", "CIV");
        gm(56, "ECU", "GER");
        gm(57, "JPN", "SWE");
        gm(58, "TUN", "NED");
        gm(59, "TUR", "USA");
        gm(60, "PAR", "AUS");
        gm(61, "NOR", "FRA");
        gm(62, "SEN", "IRQ");
        gm(63, "EGY", "IRN");
        gm(64, "NZL", "BEL");
        gm(65, "CPV", "KSA");
        gm(66, "URU", "ESP");
        gm(67, "PAN", "ENG");
        gm(68, "CRO", "GHA");
        gm(69, "ALG", "AUT");
        gm(70, "JOR", "ARG");
        gm(71, "COL", "POR");
        gm(72, "COD", "UZB");
    }

    private Wc26ScheduleCatalog() {
    }

    private static void gm(int id, String home, String away) {
        STATIC_BY_ID.put(id, new GroupMatch(id, home, away));
    }

    public static void installDbLookup(Map<Integer, GroupMatch> loaded) {
        dbById = Map.copyOf(loaded);
    }

    public static Optional<GroupMatch> findById(int scheduleId) {
        GroupMatch fromDb = dbById.get(scheduleId);
        if (fromDb != null) {
            return Optional.of(fromDb);
        }
        return Optional.ofNullable(STATIC_BY_ID.get(scheduleId));
    }

    public static Map<Integer, GroupMatch> allGroupMatches() {
        if (!dbById.isEmpty()) {
            return Collections.unmodifiableMap(dbById);
        }
        return Collections.unmodifiableMap(STATIC_BY_ID);
    }
}
