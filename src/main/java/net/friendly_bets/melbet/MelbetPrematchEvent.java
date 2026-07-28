package net.friendly_bets.melbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MelbetPrematchEvent {
    long eventId;
    String name;
    String homeTeam;
    String awayTeam;
    /** English names when present (EHT/EAT). */
    String homeTeamEn;
    String awayTeamEn;
    Instant kickoff;
    boolean outright;
    boolean live;

    public boolean isMatchEvent() {
        return !outright
                && homeTeam != null && !homeTeam.isBlank()
                && awayTeam != null && !awayTeam.isBlank();
    }

    public Long kickoffEpochMillis() {
        return kickoff != null ? kickoff.toEpochMilli() : null;
    }

    public static MelbetPrematchEvent fromJson(JsonNode node) {
        if (node == null || node.isNull() || !node.hasNonNull("Id")) {
            return null;
        }
        boolean outright = node.path("IsOUTRGT").asBoolean(false);
        String home = text(node, "HT");
        String away = text(node, "AT");
        Instant kickoff = null;
        if (node.hasNonNull("D")) {
            try {
                kickoff = Instant.parse(node.get("D").asText());
            } catch (Exception ignored) {
                kickoff = null;
            }
        }
        return MelbetPrematchEvent.builder()
                .eventId(node.get("Id").asLong())
                .name(firstNonBlank(text(node, "N"), text(node, "EGN")))
                .homeTeam(home)
                .awayTeam(away)
                .homeTeamEn(text(node, "EHT"))
                .awayTeamEn(text(node, "EAT"))
                .kickoff(kickoff)
                .outright(outright)
                .live(node.path("IsLS").asBoolean(false))
                .build();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String v = node.get(field).asText();
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
