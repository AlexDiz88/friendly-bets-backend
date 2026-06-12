package net.friendly_bets.fourscore;

import net.friendly_bets.marathonbet.MarathonbetTeamCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Русские имена команд на 4score.ru → FIFA-код (ЧМ26 и товарищеские тесты).
 */
public final class FourScoreTeamCatalog {

    private static final Map<String, List<String>> EXTRA_NAMES_BY_FIFA = new LinkedHashMap<>();

    static {
        names("CRC", "Коста-Рика", "Коста Рика");
        names("BOL", "Боливия");
        names("GUA", "Гватемала");
        names("WAL", "Уэльс");
        names("VEN", "Венесуэла");
        names("UZB", "Узбекистан");
    }

    private FourScoreTeamCatalog() {
    }

    public static Optional<String> fifaCodeForFourScoreName(String fourScoreName) {
        Optional<String> fromMarathon = MarathonbetTeamCatalog.fifaCodeForMarathonName(fourScoreName);
        if (fromMarathon.isPresent()) {
            return fromMarathon;
        }
        if (fourScoreName == null || fourScoreName.isBlank()) {
            return Optional.empty();
        }
        String norm = normalize(fourScoreName);
        for (Map.Entry<String, List<String>> entry : EXTRA_NAMES_BY_FIFA.entrySet()) {
            for (String candidate : entry.getValue()) {
                String candNorm = normalize(candidate);
                if (norm.equals(candNorm) || norm.contains(candNorm) || candNorm.contains(norm)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    private static void names(String fifaCode, String... aliases) {
        List<String> list = new ArrayList<>();
        for (String alias : aliases) {
            list.add(alias);
        }
        EXTRA_NAMES_BY_FIFA.put(fifaCode, list);
    }

    private static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
