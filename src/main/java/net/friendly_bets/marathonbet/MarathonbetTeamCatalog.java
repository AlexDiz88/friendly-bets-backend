package net.friendly_bets.marathonbet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Русские (и др.) имена сборных в линии Marathon → FIFA-код ЧМ26.
 * Имена взяты с ответа tournaments/2253726 (могут отличаться от i18n «Корея» / «Босния»).
 */
public final class MarathonbetTeamCatalog {

    private static final Map<String, List<String>> NAMES_BY_FIFA = new LinkedHashMap<>();

    static {
        names("MEX", "Мексика");
        names("RSA", "ЮАР", "Южная Африка");
        names("KOR", "Республика Корея", "Корея", "South Korea", "Korea Republic");
        names("CZE", "Чехия", "Czechia");
        names("CAN", "Канада");
        names("BIH", "Босния и Герцеговина", "Босния");
        names("USA", "США", "Соединенные Штаты");
        names("PAR", "Парагвай");
        names("SUI", "Швейцария");
        names("QAT", "Катар");
        names("BRA", "Бразилия");
        names("MAR", "Марокко");
        names("HAI", "Гаити");
        names("SCO", "Шотландия");
        names("AUS", "Австралия");
        names("TUR", "Турция");
        names("GER", "Германия");
        names("CUW", "Кюрасао");
        names("CIV", "Кот-д'Ивуар", "Кот-д’Ивуар");
        names("ECU", "Эквадор");
        names("NED", "Нидерланды");
        names("JPN", "Япония");
        names("TUN", "Тунис");
        names("SWE", "Швеция");
        names("KSA", "Саудовская Аравия", "Сауд.Аравия");
        names("URU", "Уругвай");
        names("ESP", "Испания");
        names("CPV", "Кабо-Верде");
        names("IRN", "Иран");
        names("NZL", "Новая Зеландия");
        names("BEL", "Бельгия");
        names("EGY", "Египет");
        names("FRA", "Франция");
        names("SEN", "Сенегал");
        names("IRQ", "Ирак");
        names("NOR", "Норвегия");
        names("ARG", "Аргентина");
        names("ALG", "Алжир");
        names("AUT", "Австрия");
        names("JOR", "Иордания");
        names("ENG", "Англия");
        names("CRO", "Хорватия");
        names("GHA", "Гана");
        names("PAN", "Панама");
        names("POR", "Португалия");
        names("UZB", "Узбекистан");
        names("COL", "Колумбия");
        names("COD", "ДР Конго");
    }

    private MarathonbetTeamCatalog() {
    }

    public static Optional<String> fifaCodeForMarathonName(String marathonName) {
        if (marathonName == null || marathonName.isBlank()) {
            return Optional.empty();
        }
        String norm = normalize(marathonName);
        for (Map.Entry<String, List<String>> entry : NAMES_BY_FIFA.entrySet()) {
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
        list.add(fifaCode);
        for (String alias : aliases) {
            list.add(alias);
        }
        NAMES_BY_FIFA.put(fifaCode, list);
    }

    private static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
