/**
 * Shared odds domain: provider quotes → Mongo {@code odds} → OddsPick UI.
 *
 * <p>{@link OddsMerger} группирует {@link MappedOddsQuote} по {@link BetTitleKey}.
 * Prod-рынки пишет текущий ODDS primary (сейчас Marathonbet SSE), не внешний odds-api.io.
 */
package net.friendly_bets.odds.mapping;
