package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.schedule.StandingZoneRule;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StandingZoneRuleDto {

    private String code;
    private String label;
    private String cssClass;

    public static StandingZoneRuleDto from(StandingZoneRule rule) {
        if (rule == null) {
            return null;
        }
        return StandingZoneRuleDto.builder()
                .code(rule.getCode())
                .label(rule.getLabel())
                .cssClass(rule.getCssClass())
                .build();
    }
}
