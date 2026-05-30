package net.friendly_bets.wc26;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FIFA 3-letter codes for WC26 group-stage teams → odds-api.io {@code external_name} on {@link net.friendly_bets.models.Team}.
 * Mirrors frontend {@code wc26.teams.*} display names (same strings as odds-api events).
 */
public final class Wc26TeamCatalog {

    private static final Map<String, List<String>> ODDS_API_NAMES_BY_FIFA_CODE = new LinkedHashMap<>();

    static {
        names("MEX", "Mexico");
        names("RSA", "South Africa");
        names("KOR", "Korea Republic", "South Korea");
        names("CZE", "Czechia", "Czech Republic");
        names("CAN", "Canada");
        names("SUI", "Switzerland");
        names("QAT", "Qatar");
        names("BIH", "Bosnia and Herzegovina");
        names("BRA", "Brazil");
        names("MAR", "Morocco");
        names("HAI", "Haiti");
        names("SCO", "Scotland");
        names("USA", "USA", "United States");
        names("PAR", "Paraguay");
        names("AUS", "Australia");
        names("TUR", "Türkiye", "Turkey");
        names("GER", "Germany");
        names("CUW", "Curaçao", "Curacao");
        names("CIV", "Côte d'Ivoire", "Ivory Coast");
        names("ECU", "Ecuador");
        names("NED", "Netherlands");
        names("JPN", "Japan");
        names("TUN", "Tunisia");
        names("SWE", "Sweden");
        names("KSA", "Saudi Arabia");
        names("URU", "Uruguay");
        names("ESP", "Spain");
        names("CPV", "Cabo Verde", "Cape Verde");
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
        names("AUT", "Austria");
        names("JOR", "Jordan");
        names("ENG", "England");
        names("CRO", "Croatia");
        names("GHA", "Ghana");
        names("PAN", "Panama");
        names("POR", "Portugal");
        names("UZB", "Uzbekistan");
        names("COL", "Colombia");
        names("COD", "Congo DR", "DR Congo");
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
