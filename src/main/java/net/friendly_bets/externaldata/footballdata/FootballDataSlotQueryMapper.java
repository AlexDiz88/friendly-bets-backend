package net.friendly_bets.externaldata.footballdata;

import net.friendly_bets.externaldata.ExternalSlotQuery;
import net.friendly_bets.externaldata.ExternalSlotQueryMapper;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps tournament slots to football-data.org query parameters.
 */
@Component
public class FootballDataSlotQueryMapper implements ExternalSlotQueryMapper {

    public static final String PROVIDER_ID = "football-data";

    private static final Pattern LEG_SUFFIX = Pattern.compile(" \\[(\\d)]$");

    private static final Map<String, String> PLAYOFF_STAGE_TO_API = Map.ofEntries(
            Map.entry("1/16", "PLAY_OFF_ROUND"),
            Map.entry("1/8", "LAST_16"),
            Map.entry("1/4", "QUARTER_FINALS"),
            Map.entry("1/2", "SEMI_FINALS"),
            Map.entry("final", "FINAL"),
            Map.entry("third_place", "THIRD_PLACE")
    );

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Optional<ExternalSlotQuery> map(ExpandedMatchdaySlot slot) {
        if (slot == null) {
            return Optional.empty();
        }
        return Optional.of(switch (slot.getKind()) {
            case REGULAR, GROUP -> ExternalSlotQuery.builder()
                    .queryType(ExternalSlotQuery.QueryType.MATCHDAY)
                    .matchday(Integer.parseInt(slot.getId()))
                    .build();
            case KNOCKOUT -> resolveKnockout(slot.getId());
        });
    }

    private ExternalSlotQuery resolveKnockout(String slotId) {
        final String playoffStage;
        final Integer leg;
        Matcher matcher = LEG_SUFFIX.matcher(slotId);
        if (matcher.find()) {
            leg = Integer.parseInt(matcher.group(1));
            playoffStage = slotId.substring(0, matcher.start()).trim();
        } else {
            leg = null;
            playoffStage = slotId;
        }
        String apiStage = PLAYOFF_STAGE_TO_API.get(playoffStage);
        if (apiStage == null) {
            throw new IllegalArgumentException("Unknown playoff stage for football-data: " + playoffStage);
        }
        if (leg == null) {
            return ExternalSlotQuery.builder()
                    .queryType(ExternalSlotQuery.QueryType.STAGE)
                    .stage(apiStage)
                    .build();
        }
        return ExternalSlotQuery.builder()
                .queryType(ExternalSlotQuery.QueryType.STAGE_LEG)
                .stage(apiStage)
                .leg(leg)
                .build();
    }
}
