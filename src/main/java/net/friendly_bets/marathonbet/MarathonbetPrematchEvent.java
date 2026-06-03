package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarathonbetPrematchEvent {
    long treeId;
    Long eventId;
    String name;
    String homeTeam;
    String awayTeam;
    Long displayTimeMillis;

    public static MarathonbetPrematchEvent fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long displayTime = node.hasNonNull("displayTime") ? node.get("displayTime").asLong() : null;
        return MarathonbetPrematchEvent.builder()
                .treeId(node.get("treeId").asLong())
                .eventId(node.hasNonNull("eventId") ? node.get("eventId").asLong() : null)
                .name(MarathonbetMarketExtractor.text(node.get("name")))
                .homeTeam(MarathonbetMarketExtractor.memberName(node, "homeTeam"))
                .awayTeam(MarathonbetMarketExtractor.memberName(node, "awayTeam"))
                .displayTimeMillis(displayTime)
                .build();
    }
}
