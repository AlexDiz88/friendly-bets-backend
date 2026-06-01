package net.friendly_bets.repositories;

import net.friendly_bets.models.gameresults.ApiSyncIssue;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ApiSyncIssueRepository extends MongoRepository<ApiSyncIssue, String> {

    List<ApiSyncIssue> findTop200ByOrderByCreatedAtDesc();

    boolean existsByProviderAndIssueTypeAndExternalMatchId(String provider, String issueType, Long externalMatchId);

    boolean existsByProviderAndIssueTypeAndGameResultId(String provider, String issueType, String gameResultId);

    Optional<ApiSyncIssue> findFirstByProviderAndIssueTypeAndGameResultId(
            String provider,
            String issueType,
            String gameResultId
    );

    boolean existsByIssueType(String issueType);

    boolean existsByIssueTypeIn(java.util.Collection<String> issueTypes);
}
