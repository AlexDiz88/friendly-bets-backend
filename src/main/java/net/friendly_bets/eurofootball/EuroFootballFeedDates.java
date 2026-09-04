package net.friendly_bets.eurofootball;

import net.friendly_bets.exceptions.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * euro-football.ru online day catalogue uses relative paths
 * ({@code /online/today}, {@code /yesterday}, …), not {@code /online/yyyy-MM-dd}
 * (that URL returns an empty SSR shell without match rows).
 * Calendar day of the edition is {@link #FEED_ZONE}.
 */
public final class EuroFootballFeedDates {

    /** Site edition calendar (matches «Вчера» / «Сегодня» tabs). */
    public static final ZoneId FEED_ZONE = ZoneId.of("Europe/Moscow");

    private EuroFootballFeedDates() {
    }

    public static LocalDate feedDate(Instant utcKickoff) {
        if (utcKickoff == null) {
            throw new BadRequestException("euroFootballFetchFailed");
        }
        return LocalDate.ofInstant(utcKickoff, FEED_ZONE);
    }

    public static LocalDate siteToday(Instant now) {
        Instant anchor = now != null ? now : Instant.now();
        return LocalDate.ofInstant(anchor, FEED_ZONE);
    }

    /**
     * Relative online path for a feed calendar day vs site "today".
     * Supported offsets: -2 … +2.
     */
    public static String onlinePathForFeedDate(LocalDate feedDate, LocalDate siteToday) {
        if (feedDate == null || siteToday == null) {
            throw new BadRequestException("euroFootballFetchFailed");
        }
        long offset = ChronoUnit.DAYS.between(siteToday, feedDate);
        return switch ((int) offset) {
            case 0 -> "/online/today";
            case -1 -> "/online/yesterday";
            case -2 -> "/online/before-yesterday";
            case 1 -> "/online/tomorrow";
            case 2 -> "/online/after-tomorrow";
            default -> throw new BadRequestException("euroFootballDateOutOfRange");
        };
    }
}
