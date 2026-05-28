package net.friendly_bets.footballdata;

/**
 * Ключ тура для опроса football-data.org.
 *
 * @param leagueCode внутренний код лиги ({@link net.friendly_bets.models.League.LeagueCode#name()})
 */
public record FootballDataMatchdayKey(String leagueCode, int matchday, String season, String leagueId) {
}
