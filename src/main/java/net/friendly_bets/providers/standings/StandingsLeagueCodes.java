package net.friendly_bets.providers.standings;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

import java.util.Objects;
import java.util.Optional;

public final class StandingsLeagueCodes {

    private StandingsLeagueCodes() {
    }

    public static League.LeagueCode parse(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }

    public static Optional<League> findLeague(Season season, League.LeagueCode leagueCode) {
        if (season == null || season.getLeagues() == null || leagueCode == null) {
            return Optional.empty();
        }
        return season.getLeagues().stream()
                .filter(Objects::nonNull)
                .filter(l -> leagueCode.equals(l.getLeagueCode()))
                .findFirst();
    }
}
