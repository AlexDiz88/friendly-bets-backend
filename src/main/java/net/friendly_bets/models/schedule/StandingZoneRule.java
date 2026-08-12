package net.friendly_bets.models.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StandingZoneRule {

    @Field(name = "code")
    private String code;

    @Field(name = "label")
    private String label;

    @Field(name = "css_class")
    private String cssClass;
}
