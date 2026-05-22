package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FootballDataLegFilter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    private FootballDataLegFilter() {
    }

    public static List<FootballDataMatchDto> filterByLeg(List<FootballDataMatchDto> matches, int leg) {
        Map<String, List<FootballDataMatchDto>> byTie = new LinkedHashMap<>();
        for (FootballDataMatchDto match : matches) {
            String key = tieKey(match);
            byTie.computeIfAbsent(key, k -> new ArrayList<>()).add(match);
        }
        List<FootballDataMatchDto> result = new ArrayList<>();
        for (List<FootballDataMatchDto> tieMatches : byTie.values()) {
            tieMatches.sort(Comparator.comparing(FootballDataLegFilter::parseUtcDate));
            int index = leg - 1;
            if (index >= 0 && index < tieMatches.size()) {
                result.add(tieMatches.get(index));
            }
        }
        return result;
    }

    private static String tieKey(FootballDataMatchDto match) {
        int home = match.getHomeTeam().getId();
        int away = match.getAwayTeam().getId();
        int min = Math.min(home, away);
        int max = Math.max(home, away);
        return min + ":" + max;
    }

    private static LocalDateTime parseUtcDate(FootballDataMatchDto match) {
        if (match.getUtcDate() == null || match.getUtcDate().isBlank()) {
            return LocalDateTime.MIN;
        }
        String raw = match.getUtcDate();
        if (raw.endsWith("Z")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return LocalDateTime.parse(raw, ISO);
    }
}
