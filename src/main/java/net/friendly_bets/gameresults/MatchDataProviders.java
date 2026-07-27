package net.friendly_bets.gameresults;

public final class MatchDataProviders {

    /** Логический id провайдера (совпадает с {@link net.friendly_bets.models.TeamExternalAlias}). */
    public static final String MARATHONBET = "marathonbet";
    public static final String TWENTYFOUR_SCORE = "24score.pro";
    public static final String SOCCER365 = "soccer365.ru";

    private MatchDataProviders() {
    }

    /** Ключ в {@code match_schedules.external_ids} (без точек в имени Mongo-поля). */
    public static String sourcesStorageKey(String providerId) {
        if (providerId == null) {
            return null;
        }
        if (TWENTYFOUR_SCORE.equals(providerId)) {
            return "24score";
        }
        if (SOCCER365.equals(providerId)) {
            return "soccer365";
        }
        return providerId.replace('-', '_');
    }
}
