package net.friendly_bets.melbet;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class MelbetTournamentParser {

    private MelbetTournamentParser() {
    }

    /**
     * Decrypted GetMixed body: object with {@code events[]} (or bare array).
     */
    public static List<MelbetPrematchEvent> parsePrematchEvents(JsonNode body) {
        if (body == null || body.isNull()) {
            return List.of();
        }
        JsonNode eventsNode = body.has("events") ? body.get("events") : body;
        if (eventsNode == null || !eventsNode.isArray()) {
            return List.of();
        }
        List<MelbetPrematchEvent> result = new ArrayList<>();
        for (JsonNode node : eventsNode) {
            MelbetPrematchEvent event = MelbetPrematchEvent.fromJson(node);
            if (event != null && event.isMatchEvent() && !event.isLive()) {
                result.add(event);
            }
        }
        return result;
    }
}
