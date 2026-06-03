package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetScrapeResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.marathonbet.client.MarathonbetEventLineClient;
import net.friendly_bets.marathonbet.client.MarathonbetPanHeaders;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarathonbetScrapeService {

    private static final String BASE_URL = MarathonbetPanHeaders.BASE_URL;

    private final MarathonbetEventLineClient eventLineClient;
    private final MarathonbetProperties properties;

    public MarathonbetScrapeResultDto scrapeByTreeId(long treeId) {
        if (treeId <= 0) {
            throw new BadRequestException("marathonbetInvalidTreeId");
        }
        JsonNode root = eventLineClient.fetchEventSnapshot(treeId);
        Set<String> models = new HashSet<>();
        if (properties.getTournamentMarketModels() != null) {
            models.addAll(properties.getTournamentMarketModels());
        }
        if (properties.getSseExtraMarketModels() != null) {
            models.addAll(properties.getSseExtraMarketModels());
        }
        models.add("MTCH_DC");
        MarathonbetExtractedMarkets extracted = MarathonbetMarketExtractor.extract(root, models);

        List<String> warnings = new ArrayList<>();
        if (extracted.getMatchResultMarkets().isEmpty()) {
            warnings.add("matchResultNotFound");
        }
        if (extracted.getHandicapMarkets().isEmpty()) {
            warnings.add("handicapNotFound");
        }
        if (extracted.getTotalMarkets().isEmpty()) {
            warnings.add("totalNotFound");
        }
        if (extracted.getCorrectScoreMarkets().isEmpty()) {
            warnings.add("correctScoreNotFound");
        }

        Long displayTime = root.hasNonNull("displayTime") ? root.get("displayTime").asLong() : null;
        Instant startTime = displayTime != null ? Instant.ofEpochMilli(displayTime) : null;

        return MarathonbetScrapeResultDto.builder()
                .treeId(treeId)
                .eventId(root.hasNonNull("eventId") ? root.get("eventId").asLong() : null)
                .eventName(MarathonbetMarketExtractor.text(root.get("name")))
                .competitionHeader(MarathonbetMarketExtractor.text(root.get("header")))
                .homeTeam(MarathonbetMarketExtractor.memberName(root, "homeTeam"))
                .awayTeam(MarathonbetMarketExtractor.memberName(root, "awayTeam"))
                .startTime(startTime)
                .sourceUrl(BASE_URL + "/su/sport/event/" + treeId)
                .fetchedAt(Instant.now())
                .matchResultMarkets(extracted.getMatchResultMarkets())
                .handicapMarkets(extracted.getHandicapMarkets())
                .totalMarkets(extracted.getTotalMarkets())
                .correctScoreMarkets(extracted.getCorrectScoreMarkets())
                .doubleChanceMarkets(extracted.getDoubleChanceMarkets())
                .warnings(warnings)
                .build();
    }

    public JsonNode fetchEventSnapshot(long treeId) {
        return eventLineClient.fetchEventSnapshot(treeId);
    }
}
