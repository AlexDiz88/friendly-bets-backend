package net.friendly_bets.melbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MelbetHttpFetchResult {
    boolean success;
    Integer httpStatus;
    MelbetHttpOutcome outcome;
    long durationMs;
    JsonNode body;
    String errorDetail;
    Integer retryAfterSeconds;

    public String toErrorKey() {
        if (outcome == null) {
            return "melbetFetchFailed";
        }
        return switch (outcome) {
            case TIMEOUT -> "melbetTimeout";
            case DECRYPT_ERROR -> "melbetDecryptFailed";
            case PARSE_ERROR -> "melbetParseFailed";
            case HTTP_ERROR -> "melbetHttpError";
            case NETWORK_ERROR -> "melbetNetworkError";
            case SUCCESS -> "melbetFetchFailed";
        };
    }
}
