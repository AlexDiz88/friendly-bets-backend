package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TeamDisplayNames {

    @Field(name = "en")
    private String en;

    @Field(name = "ru")
    private String ru;

    @Field(name = "de")
    private String de;
}
