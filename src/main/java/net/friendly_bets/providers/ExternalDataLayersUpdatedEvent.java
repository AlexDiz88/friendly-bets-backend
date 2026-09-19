package net.friendly_bets.providers;

/**
 * Published after {@code app_settings.external_data_layers} changes (admin PATCH or circuit breaker).
 * LIVE wake must re-evaluate: re-enable after {@code enabled=false} previously cancelled the future.
 */
public class ExternalDataLayersUpdatedEvent {
}
