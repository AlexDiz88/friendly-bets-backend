package net.friendly_bets.models.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Home/away titles + logo keys for monitoring / error-log UI (logo left of name).
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExternalApiMatchTeamsRef {

    @Field(name = "match_schedule_id")
    private String matchScheduleId;

    @Field(name = "home_title")
    private String homeTitle;

    @Field(name = "away_title")
    private String awayTitle;

    @Field(name = "home_logo_key")
    private String homeLogoKey;

    @Field(name = "away_logo_key")
    private String awayLogoKey;

    public String label() {
        String h = homeTitle != null ? homeTitle.trim() : "";
        String a = awayTitle != null ? awayTitle.trim() : "";
        if (h.isEmpty() && a.isEmpty()) {
            return null;
        }
        if (h.isEmpty()) {
            return a;
        }
        if (a.isEmpty()) {
            return h;
        }
        return h + " - " + a;
    }
}
