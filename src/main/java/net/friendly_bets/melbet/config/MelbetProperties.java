package net.friendly_bets.melbet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "melbet")
public class MelbetProperties {

    private String baseUrl = "https://sport.melbet.ru";
    private String referer = "https://www.melbet.ru/";
    private long partnerId = 3_000_057L;
    private String partnerUuid = "35c6d708-2caa-464a-bab5-a6656b2b80f3";
    private int langId = 1;
    /** Node executable for Digitain WASM decrypt (decrypt-cli.cjs). */
    private String nodeExecutable = "node";
    private int syncJitterMinutes = 5;
    private String scheduleZone = "Europe/Berlin";
    private Map<String, String> leagueScheduleHours = new HashMap<>();
    private Map<String, Long> tournamentIds = new HashMap<>();
    private int stageSize = 5;
    private int stagePauseMinutes = 20;
    private int eventWindowHours = 12;
    private long eventDelayMinMs = 2_000L;
    private long eventDelayMaxMs = 5_500L;

    public List<Integer> scheduleHoursForLeague(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank() || leagueScheduleHours == null) {
            return List.of();
        }
        String raw = leagueScheduleHours.get(leagueCode);
        if (raw == null) {
            raw = leagueScheduleHours.get(leagueCode.toUpperCase(Locale.ROOT));
        }
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> hours = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int hour = Integer.parseInt(trimmed);
                if (hour >= 0 && hour <= 23) {
                    hours.add(hour);
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return hours;
    }

    public boolean isLeagueHourDue(String leagueCode, int hourOfDay) {
        return scheduleHoursForLeague(leagueCode).contains(hourOfDay);
    }

    public Long tournamentIdForLeague(String leagueCode) {
        if (leagueCode == null || tournamentIds == null) {
            return null;
        }
        Long id = tournamentIds.get(leagueCode);
        if (id == null) {
            id = tournamentIds.get(leagueCode.toUpperCase(Locale.ROOT));
        }
        return id;
    }
}
