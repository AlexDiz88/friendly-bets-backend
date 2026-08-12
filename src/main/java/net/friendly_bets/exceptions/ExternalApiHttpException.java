package net.friendly_bets.exceptions;

/**
 * Outbound HTTP/transport failure while calling an external data provider.
 * Used by {@link net.friendly_bets.providers.LayerProviderRouter} to decide primary → secondary failover.
 */
public class ExternalApiHttpException extends BadRequestException {

    public ExternalApiHttpException(String message) {
        super(message);
    }
}
