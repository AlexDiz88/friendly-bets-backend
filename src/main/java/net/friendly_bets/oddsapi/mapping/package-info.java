/**
 * Shared odds domain for Marathonbet scrape → Mongo merged odds → OddsPick UI.
 *
 * <p>{@link OddsMerger} группирует {@link MappedOddsQuote} по {@link BetTitleKey}.
 * Prod-форы и рынки приходят из Marathonbet SSE, не из внешнего odds-api.io.
 */
package net.friendly_bets.oddsapi.mapping;
