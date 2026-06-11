package net.friendly_bets.gameresults;

public final class MatchDataProviders {

    /** Логический id провайдера (совпадает с {@link net.friendly_bets.models.TeamExternalAlias}). */
    public static final String FOOTBALL_DATA = "football-data";
    public static final String ODDS_API = "odds-api.io";
    public static final String API_FOOTBALL = "api-football";
    public static final String MARATHONBET = "marathonbet";
    public static final String FOURSCORE = "4score.ru";

    private MatchDataProviders() {
    }

    /** Ключ вложенного документа {@code sources} в MongoDB (без точек в имени поля). */
    public static String sourcesStorageKey(String providerId) {
        if (providerId == null) {
            return null;
        }
        return providerId.replace('-', '_');
    }
}
