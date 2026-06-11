package net.friendly_bets.marathonbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import net.friendly_bets.exceptions.BadRequestException;

@Value
@Builder
public class MarathonbetHttpFetchResult {

    boolean success;
    Integer httpStatus;
    MarathonbetHttpOutcome outcome;
    long durationMs;
    JsonNode body;
    String errorDetail;
    Integer retryAfterSeconds;

    public JsonNode requireBody() {
        if (!success) {
            throw new BadRequestException(toErrorKey());
        }
        return body;
    }

    public String toErrorKey() {
        if (httpStatus != null) {
            if (httpStatus == 429) {
                return "marathonbetRateLimited";
            }
            if (httpStatus == 403) {
                return "marathonbetAccessDenied";
            }
        }
        if (outcome == MarathonbetHttpOutcome.PARSE_ERROR) {
            return "marathonbetParseFailed";
        }
        return "marathonbetFetchFailed";
    }
}
