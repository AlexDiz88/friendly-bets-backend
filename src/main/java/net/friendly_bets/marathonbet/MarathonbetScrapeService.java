package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.dto.MarathonbetMarketSelectionDto;
import net.friendly_bets.dto.MarathonbetScrapeResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.marathonbet.client.MarathonbetEventLineClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarathonbetScrapeService {

    private static final String BASE_URL = "https://new.marathonbet.ru";

    private final MarathonbetEventLineClient eventLineClient;

    public MarathonbetScrapeResultDto scrapeByTreeId(long treeId) {
        if (treeId <= 0) {
            throw new BadRequestException("marathonbetInvalidTreeId");
        }
        JsonNode root = eventLineClient.fetchEventSnapshot(treeId);
        JsonNode marketsNode = root.get("markets");
        if (marketsNode == null || !marketsNode.isObject()) {
            throw new BadRequestException("marathonbetParseFailed");
        }

        List<MarathonbetMarketDto> matchResult = new ArrayList<>();
        List<MarathonbetMarketDto> handicaps = new ArrayList<>();
        Iterator<String> fieldNames = marketsNode.fieldNames();
        while (fieldNames.hasNext()) {
            JsonNode market = marketsNode.get(fieldNames.next());
            if (market == null || market.isNull()) {
                continue;
            }
            String model = text(market.get("model"));
            MarathonbetMarketDto dto = toMarketDto(market);
            if ("MTCH_R".equals(model)) {
                matchResult.add(dto);
            } else if ("MTCH_HB".equals(model)) {
                handicaps.add(dto);
            }
        }

        handicaps.sort(Comparator.comparing(MarathonbetMarketDto::getName));

        List<String> warnings = new ArrayList<>();
        if (matchResult.isEmpty()) {
            warnings.add("matchResultNotFound");
        }
        if (handicaps.isEmpty()) {
            warnings.add("handicapNotFound");
        }

        Long displayTime = root.hasNonNull("displayTime") ? root.get("displayTime").asLong() : null;
        Instant startTime = displayTime != null ? Instant.ofEpochMilli(displayTime) : null;

        return MarathonbetScrapeResultDto.builder()
                .treeId(treeId)
                .eventId(root.hasNonNull("eventId") ? root.get("eventId").asLong() : null)
                .eventName(text(root.get("name")))
                .competitionHeader(text(root.get("header")))
                .homeTeam(memberName(root, "homeTeam"))
                .awayTeam(memberName(root, "awayTeam"))
                .startTime(startTime)
                .sourceUrl(BASE_URL + "/su/sport/event/" + treeId)
                .fetchedAt(Instant.now())
                .matchResultMarkets(matchResult)
                .handicapMarkets(handicaps)
                .warnings(warnings)
                .build();
    }

    private static MarathonbetMarketDto toMarketDto(JsonNode market) {
        List<MarathonbetMarketSelectionDto> selections = new ArrayList<>();
        JsonNode selectionsNode = market.get("selections");
        if (selectionsNode != null && selectionsNode.isObject()) {
            Iterator<String> ids = selectionsNode.fieldNames();
            while (ids.hasNext()) {
                JsonNode sel = selectionsNode.get(ids.next());
                if (sel == null || sel.isNull()) {
                    continue;
                }
                BigDecimal odds = decimalOdds(sel.get("coeff"));
                selections.add(MarathonbetMarketSelectionDto.builder()
                        .name(text(sel.get("name")))
                        .odds(odds)
                        .build());
            }
        }
        return MarathonbetMarketDto.builder()
                .model(text(market.get("model")))
                .name(text(market.get("name")))
                .selections(selections)
                .build();
    }

    private static BigDecimal decimalOdds(JsonNode coeffNode) {
        if (coeffNode == null || coeffNode.isNull()) {
            return null;
        }
        JsonNode price = coeffNode.get("price");
        if (price == null || !price.has("n") || !price.has("d")) {
            return null;
        }
        int n = price.get("n").asInt();
        int d = price.get("d").asInt();
        if (d == 0) {
            return null;
        }
        return BigDecimal.valueOf(1.0 + (double) n / d).setScale(3, RoundingMode.HALF_UP);
    }

    private static String memberName(JsonNode root, String teamField) {
        JsonNode team = root.get(teamField);
        if (team == null) {
            return null;
        }
        JsonNode members = team.get("members");
        if (members == null || !members.isArray() || members.isEmpty()) {
            return null;
        }
        return text(members.get(0).get("name"));
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }
}
