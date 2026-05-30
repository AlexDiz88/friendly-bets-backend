package net.friendly_bets.wc26;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Group-stage WC26 schedule ids 1–72 (home/away FIFA codes). Mirrors frontend wc26Schedule.
 */
public final class Wc26ScheduleCatalog {

    public record Wc26ScheduleEntry(int id, String homeCode, String awayCode) {
    }

    private static final Map<Integer, Wc26ScheduleEntry> BY_ID = new LinkedHashMap<>();

    static {
        entry(1, "MEX", "RSA");
        entry(2, "KOR", "CZE");
        entry(3, "CAN", "BIH");
        entry(4, "USA", "PAR");
        entry(5, "HAI", "SCO");
        entry(6, "AUS", "TUR");
        entry(7, "BRA", "MAR");
        entry(8, "QAT", "SUI");
        entry(9, "CIV", "ECU");
        entry(10, "GER", "CUW");
        entry(11, "NED", "JPN");
        entry(12, "SWE", "TUN");
        entry(13, "KSA", "URU");
        entry(14, "ESP", "CPV");
        entry(15, "IRN", "NZL");
        entry(16, "BEL", "EGY");
        entry(17, "FRA", "SEN");
        entry(18, "IRQ", "NOR");
        entry(19, "ARG", "ALG");
        entry(20, "AUT", "JOR");
        entry(21, "GHA", "PAN");
        entry(22, "ENG", "CRO");
        entry(23, "POR", "COD");
        entry(24, "UZB", "COL");
        entry(25, "CZE", "RSA");
        entry(26, "SUI", "BIH");
        entry(27, "CAN", "QAT");
        entry(28, "MEX", "KOR");
        entry(29, "BRA", "HAI");
        entry(30, "SCO", "MAR");
        entry(31, "TUR", "PAR");
        entry(32, "USA", "AUS");
        entry(33, "GER", "CIV");
        entry(34, "ECU", "CUW");
        entry(35, "NED", "SWE");
        entry(36, "TUN", "JPN");
        entry(37, "URU", "CPV");
        entry(38, "ESP", "KSA");
        entry(39, "BEL", "IRN");
        entry(40, "NZL", "EGY");
        entry(41, "NOR", "SEN");
        entry(42, "FRA", "IRQ");
        entry(43, "ARG", "AUT");
        entry(44, "JOR", "ALG");
        entry(45, "ENG", "GHA");
        entry(46, "PAN", "CRO");
        entry(47, "POR", "UZB");
        entry(48, "COL", "COD");
        entry(49, "SCO", "BRA");
        entry(50, "MAR", "HAI");
        entry(51, "SUI", "CAN");
        entry(52, "BIH", "QAT");
        entry(53, "CZE", "MEX");
        entry(54, "RSA", "KOR");
        entry(55, "CUW", "CIV");
        entry(56, "ECU", "GER");
        entry(57, "JPN", "SWE");
        entry(58, "TUN", "NED");
        entry(59, "TUR", "USA");
        entry(60, "PAR", "AUS");
        entry(61, "NOR", "FRA");
        entry(62, "SEN", "IRQ");
        entry(63, "EGY", "IRN");
        entry(64, "NZL", "BEL");
        entry(65, "CPV", "KSA");
        entry(66, "URU", "ESP");
        entry(67, "PAN", "ENG");
        entry(68, "CRO", "GHA");
        entry(69, "ALG", "AUT");
        entry(70, "JOR", "ARG");
        entry(71, "COL", "POR");
        entry(72, "COD", "UZB");
    }

    private Wc26ScheduleCatalog() {
    }

    private static void entry(int id, String home, String away) {
        BY_ID.put(id, new Wc26ScheduleEntry(id, home, away));
    }

    public static Optional<Wc26ScheduleEntry> find(int scheduleId) {
        return Optional.ofNullable(BY_ID.get(scheduleId));
    }

    public static boolean isGroupStage(int scheduleId) {
        return scheduleId >= 1 && scheduleId <= 72;
    }
}
