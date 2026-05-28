package net.friendly_bets.gameresults;

public final class MatchDataProviders {

    /** Логический id провайдера (совпадает с {@link net.friendly_bets.models.TeamExternalAlias}). */
    public static final String FOOTBALL_DATA = "football-data";

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
