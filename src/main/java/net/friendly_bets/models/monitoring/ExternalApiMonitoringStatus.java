package net.friendly_bets.models.monitoring;

/**
 * Status of one external-API sync run persisted in {@code external_api_monitoring}.
 */
public enum ExternalApiMonitoringStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    SKIPPED
}
