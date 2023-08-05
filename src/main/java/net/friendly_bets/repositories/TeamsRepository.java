package net.friendly_bets.repositories;

import net.friendly_bets.models.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface TeamsRepository extends JpaRepository<Team, Long> {

    boolean existsByFullTitleRuOrFullTitleEn (String titleRu, String titleEn);

}
