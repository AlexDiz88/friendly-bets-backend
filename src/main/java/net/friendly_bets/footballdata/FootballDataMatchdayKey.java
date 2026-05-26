package net.friendly_bets.footballdata;

/**
 * Ключ тура для опроса football-data.org.
 */
public record FootballDataMatchdayKey(String competitionCode, int matchday, String season, String leagueId) {
}
