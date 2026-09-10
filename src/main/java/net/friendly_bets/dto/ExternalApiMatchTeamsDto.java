package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.monitoring.ExternalApiMatchTeamsRef;

@Value
@Builder
public class ExternalApiMatchTeamsDto {
    String matchScheduleId;
    String homeTitle;
    String awayTitle;
    String homeLogoKey;
    String awayLogoKey;

    public static ExternalApiMatchTeamsDto from(ExternalApiMatchTeamsRef ref) {
        if (ref == null) {
            return null;
        }
        return ExternalApiMatchTeamsDto.builder()
                .matchScheduleId(ref.getMatchScheduleId())
                .homeTitle(ref.getHomeTitle())
                .awayTitle(ref.getAwayTitle())
                .homeLogoKey(ref.getHomeLogoKey())
                .awayLogoKey(ref.getAwayLogoKey())
                .build();
    }
}
