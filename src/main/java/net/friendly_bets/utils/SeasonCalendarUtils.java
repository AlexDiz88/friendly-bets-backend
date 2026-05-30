package net.friendly_bets.utils;

import net.friendly_bets.exceptions.BadRequestException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SeasonCalendarUtils {

    private SeasonCalendarUtils() {
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("seasonDatesRequired");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("seasonEndDateBeforeStartDate");
        }
    }

    /**
     * Год для football-data.org: сезон 2024/25 → {@code 2024} (год старта).
     */
    public static Integer resolveExternalSeasonYear(LocalDate startDate) {
        if (startDate == null) {
            return null;
        }
        return startDate.getYear();
    }

    /**
     * Год турнира для football-data.org (WC, EC): ЧМ-2026 → {@code 2026} (год окончания сезона / проведения).
     */
    public static Integer resolveTournamentExternalSeasonYear(LocalDate startDate, LocalDate endDate) {
        if (endDate != null) {
            return endDate.getYear();
        }
        return resolveExternalSeasonYear(startDate);
    }

    public static List<Integer> availableExternalYears(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        List<Integer> years = new ArrayList<>();
        for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
            years.add(year);
        }
        return years;
    }
}
