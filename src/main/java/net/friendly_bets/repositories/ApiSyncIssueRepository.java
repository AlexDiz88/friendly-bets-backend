package net.friendly_bets.repositories;

import net.friendly_bets.models.gameresults.ApiSyncIssue;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApiSyncIssueRepository extends MongoRepository<ApiSyncIssue, String> {

    List<ApiSyncIssue> findTop200ByOrderByCreatedAtDesc();

    boolean existsByProviderAndIssueTypeAndExternalMatchId(String provider, String issueType, Long externalMatchId);
}
