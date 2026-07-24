package net.friendly_bets.models.tournamentarchive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Пара сетки: в матч {@code matchNumber} выходят победители (или проигравшие) матчей {@code home}/{@code away}.
 * {@code from}: winner | runner_up
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TournamentArchiveBracketPair {

    @Field(name = "match_number")
    private int matchNumber;

    /** Номер матча, победитель/проигравший которого играет дома. */
    @Field(name = "home")
    private Integer home;

    /** Номер матча, победитель/проигравший которого играет в гостях. */
    @Field(name = "away")
    private Integer away;

    /** winner (по умолчанию) | runner_up (матч за 3-е). */
    @Field(name = "from")
    private String from;
}
