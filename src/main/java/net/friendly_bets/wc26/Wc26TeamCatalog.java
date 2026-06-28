package net.friendly_bets.wc26;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * FIFA 3-letter codes for WC26 group-stage teams → odds-api.io {@code external_name} on {@link net.friendly_bets.models.Team}.
 * Mirrors frontend {@code wc26.teams.*} display names (same strings as odds-api events).
 */
public final class Wc26TeamCatalog {

    private static final Map<String, List<String>> ODDS_API_NAMES_BY_FIFA_CODE = new LinkedHashMap<>();

    static {
        names("MEX", "Mexico", "Мексика");
        names("RSA", "South Africa", "SouthAfrica", "ЮАР", "Южная Африка");
        names("KOR", "Korea Republic", "South Korea", "KoreaRepublic", "SouthKorea", "Корея", "Южная Корея");
        names("CZE", "Czechia", "Czech Republic", "CzechRepublic", "Чехия");
        names("CAN", "Canada");
        names("SUI", "Switzerland");
        names("QAT", "Qatar");
        names("BIH", "Bosnia and Herzegovina", "Bosnia", "BosniaHerzegovina");
        names("BRA", "Brazil");
        names("MAR", "Morocco");
        names("HAI", "Haiti");
        names("SCO", "Scotland");
        names("USA", "USA", "United States", "UnitedStates");
        names("PAR", "Paraguay");
        names("AUS", "Australia");
        names("TUR", "Türkiye", "Turkey");
        names("GER", "Germany");
        names("CUW", "Curaçao", "Curacao");
        names("CIV", "Côte d'Ivoire", "Ivory Coast", "IvoryCoast");
        names("ECU", "Ecuador");
        names("NED", "Netherlands");
        names("JPN", "Japan");
        names("TUN", "Tunisia");
        names("SWE", "Sweden");
        names("KSA", "Saudi Arabia", "SaudiArabia");
        names("URU", "Uruguay");
        names("ESP", "Spain", "Испания");
        names("CPV", "Cabo Verde", "Cape Verde", "Cape Verde Islands", "CaboVerde");
        names("IRN", "IR Iran", "Iran");
        names("NZL", "New Zealand");
        names("BEL", "Belgium");
        names("EGY", "Egypt");
        names("FRA", "France");
        names("SEN", "Senegal");
        names("IRQ", "Iraq");
        names("NOR", "Norway");
        names("ARG", "Argentina");
        names("ALG", "Algeria");
        names("AUT", "Austria", "Австрия");
        names("JOR", "Jordan");
        names("ENG", "England");
        names("CRO", "Croatia");
        names("GHA", "Ghana");
        names("PAN", "Panama");
        names("POR", "Portugal");
        names("UZB", "Uzbekistan");
        names("COL", "Colombia");
        names("COD", "Congo DR", "DR Congo", "DRCongo");
    }

    public static Optional<String> fifaCodeForKnownName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String compact = normalizeCompact(name);
        for (Map.Entry<String, List<String>> entry : ODDS_API_NAMES_BY_FIFA_CODE.entrySet()) {
            if (normalizeCompact(entry.getKey()).equals(compact)) {
                return Optional.of(entry.getKey());
            }
            for (String candidate : entry.getValue()) {
                if (normalizeCompact(candidate).equals(compact)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    public static boolean nameMatchesFifaCode(String name, String tla, String fifaCode) {
        if (fifaCode == null || fifaCode.isBlank()) {
            return false;
        }
        if (tla != null && !tla.isBlank() && tla.trim().equalsIgnoreCase(fifaCode)) {
            return true;
        }
        return fifaCodeForKnownName(name)
                .map(code -> code.equalsIgnoreCase(fifaCode))
                .orElse(false);
    }

    public static String normalizeCompact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Wc26TeamCatalog() {
    }

    private static void names(String fifaCode, String primary, String... alternates) {
        List<String> list = new ArrayList<>();
        list.add(primary);
        for (String alternate : alternates) {
            list.add(alternate);
        }
        ODDS_API_NAMES_BY_FIFA_CODE.put(fifaCode, List.copyOf(list));
    }

    public static List<String> oddsApiNameCandidatesForFifaCode(String fifaCode) {
        if (fifaCode == null || fifaCode.isBlank()) {
            return List.of();
        }
        return ODDS_API_NAMES_BY_FIFA_CODE.getOrDefault(fifaCode.trim(), List.of());
    }
}
