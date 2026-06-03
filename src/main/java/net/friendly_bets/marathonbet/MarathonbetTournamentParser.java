package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class MarathonbetTournamentParser {

    private MarathonbetTournamentParser() {
    }

    public static List<MarathonbetPrematchEvent> parsePrematchEvents(JsonNode tournamentRoot) {
        if (tournamentRoot == null) {
            return List.of();
        }
        JsonNode events = tournamentRoot.get("prematchEvents");
        if (events == null || !events.isArray()) {
            return List.of();
        }
        List<MarathonbetPrematchEvent> result = new ArrayList<>();
        for (JsonNode node : events) {
            MarathonbetPrematchEvent event = MarathonbetPrematchEvent.fromJson(node);
            if (event != null) {
                result.add(event);
            }
        }
        return result;
    }

    public static JsonNode eventNodeByTreeId(JsonNode tournamentRoot, long treeId) {
        if (tournamentRoot == null) {
            return null;
        }
        JsonNode events = tournamentRoot.get("prematchEvents");
        if (events == null || !events.isArray()) {
            return null;
        }
        for (JsonNode node : events) {
            if (node.hasNonNull("treeId") && node.get("treeId").asLong() == treeId) {
                return node;
            }
        }
        return null;
    }
}
