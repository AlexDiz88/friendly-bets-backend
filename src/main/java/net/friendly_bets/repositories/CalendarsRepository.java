package net.friendly_bets.repositories;

import net.friendly_bets.models.CalendarNode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface CalendarsRepository extends MongoRepository<CalendarNode, String> {

    Optional<List<CalendarNode>> findBySeasonId(String seasonId);

    Optional<List<CalendarNode>> findBySeasonIdAndHasBets(String seasonId, Boolean hasBets);

}
