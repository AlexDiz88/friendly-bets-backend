package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.client.MarathonbetEventLineClient;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarathonbetScrapeService {

    private final MarathonbetEventLineClient eventLineClient;

    public MarathonbetHttpFetchResult fetchEventSnapshotResult(long treeId) {
        return eventLineClient.fetchEventSnapshot(treeId);
    }
}
