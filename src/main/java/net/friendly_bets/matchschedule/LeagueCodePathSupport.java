package net.friendly_bets.matchschedule;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;

/** Преобразование path/query параметра во внутренний код лиги ({@link League.LeagueCode#name()}). */
public final class LeagueCodePathSupport {

    private LeagueCodePathSupport() {
    }

    public static String resolveStorageLeagueCode(String pathParam) {
        if (pathParam == null || pathParam.isBlank()) {
            throw new BadRequestException("unknownLeagueCode");
        }
        try {
            return League.LeagueCode.valueOf(pathParam.trim()).name();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unknownLeagueCode");
        }
    }
}
