package net.friendly_bets.externaldata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;

import java.util.List;

/**
 * Fetches external match lists for a competition slot (football-data.org today; other APIs later).
 */
public interface ExternalMatchDataProvider {

    String providerId();

    /** Lower value = higher priority when multiple providers are configured. */
    int priority();

    boolean isAvailable();

    List<FootballDataMatchDto> fetchMatches(ExternalMatchFetchRequest request);
}
