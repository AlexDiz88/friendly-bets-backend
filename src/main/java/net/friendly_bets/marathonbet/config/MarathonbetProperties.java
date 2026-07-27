package net.friendly_bets.marathonbet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "marathonbet")
public class MarathonbetProperties {

    /** Legacy standalone scheduler interval (unused when league scheduler is active). */
    private long syncIntervalMs = 43_200_000L;
    /** Random delay (0..N minutes) after mutex is acquired for a due league. */
    private int syncJitterMinutes = 5;
    private Map<String, Long> tournamentTreeIds = new HashMap<>(Map.of("WC", 2_253_726L));
    /** Zone for league schedule hours, e.g. Europe/Berlin. */
    private String scheduleZone = "Europe/Berlin";
    /**
     * Hours of day (0–23) when each league may sync, comma-separated.
     * Example: {@code marathonbet.league-schedule-hours.EPL=0,12}
     */
    private Map<String, String> leagueScheduleHours = new HashMap<>();
    /** Max matches per SSE stage (sorted by kickoff). */
    private int stageSize = 5;
    /** Pause between SSE stages (minutes). */
    private int stagePauseMinutes = 20;
    /** Re-fetch SSE only if odds missing or kickoff within this many hours. */
    private int sseRefreshWithinHours = 36;
    private int eventWindowHours = 6;
    /** Random pause lower bound between per-event SSE fetches (ms). */
    private long sseDelayMinMs = 2_000L;
    /** Random pause upper bound between per-event SSE fetches (ms). */
    private long sseDelayMaxMs = 5_500L;

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
                // skip invalid token
            }
        }
        return hours;
    }

    public boolean isLeagueHourDue(String leagueCode, int hourOfDay) {
        return scheduleHoursForLeague(leagueCode).contains(hourOfDay);
    }

    public Long tournamentTreeIdForLeague(String leagueCode) {
        if (leagueCode == null || tournamentTreeIds == null) {
            return null;
        }
        Long id = tournamentTreeIds.get(leagueCode);
        if (id == null) {
            id = tournamentTreeIds.get(leagueCode.toUpperCase(Locale.ROOT));
        }
        return id;
    }
}
