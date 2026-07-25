package net.friendly_bets.providers;

/**
 * External data responsibility layers. Each layer has its own primary (and optional secondary) provider.
 */
public enum ExternalDataLayer {
    SCHEDULE,
    ODDS,
    LIVE,
    FULL_MATCH
}
