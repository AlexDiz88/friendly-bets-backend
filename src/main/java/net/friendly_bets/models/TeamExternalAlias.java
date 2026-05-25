package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Связь внутренней команды с идентификатором/названием во внешнем API.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TeamExternalAlias {

    /** Например {@code football-data}. */
    @Field(name = "provider")
    private String provider;

    @Field(name = "external_id")
    private Integer externalId;

    @Field(name = "external_name")
    private String externalName;
}
