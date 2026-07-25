package net.friendly_bets.models.monitoring;

/**
 * How the sync was triggered.
 */
public enum ExternalApiMonitoringTrigger {
    CRON,
    ADMIN,
    ORCHESTRATOR
}
