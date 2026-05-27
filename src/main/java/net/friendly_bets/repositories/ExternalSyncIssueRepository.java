package net.friendly_bets.repositories;

import net.friendly_bets.models.external.ExternalSyncIssue;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExternalSyncIssueRepository extends MongoRepository<ExternalSyncIssue, String> {
    List<ExternalSyncIssue> findTop200ByOrderByCreatedAtDesc();

    boolean existsByProviderAndIssueTypeAndExternalMatchId(String provider, String issueType, Long externalMatchId);
}

