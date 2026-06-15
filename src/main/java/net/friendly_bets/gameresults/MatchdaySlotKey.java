package net.friendly_bets.gameresults;

/**
 * Ключ тура для опроса внешних провайдеров результатов.
 *
 * @param leagueCode внутренний код лиги ({@link net.friendly_bets.models.League.LeagueCode#name()})
 */
public record MatchdaySlotKey(String leagueCode, int matchday, String season, String leagueId) {
}
