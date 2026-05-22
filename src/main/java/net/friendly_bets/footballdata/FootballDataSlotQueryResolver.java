package net.friendly_bets.footballdata;

import net.friendly_bets.models.ExpandedMatchdaySlot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FootballDataSlotQueryResolver {

    private static final Pattern LEG_SUFFIX = Pattern.compile(" \\[(\\d)]$");

    /**
     * Стадия в формате FriendlyBets → stage football-data.org
     */
    private static final Map<String, String> PLAYOFF_STAGE_TO_API = Map.ofEntries(
            Map.entry("1/16", "PLAY_OFF_ROUND"),
            Map.entry("1/8", "LAST_16"),
            Map.entry("1/4", "QUARTER_FINALS"),
            Map.entry("1/2", "SEMI_FINALS"),
            Map.entry("final", "FINAL"),
            Map.entry("third_place", "THIRD_PLACE")
    );

    public FootballDataSlotQuery resolve(ExpandedMatchdaySlot slot) {
        return switch (slot.getKind()) {
            case REGULAR, GROUP -> FootballDataSlotQuery.builder()
                    .queryType(FootballDataSlotQuery.QueryType.MATCHDAY)
                    .matchday(Integer.parseInt(slot.getId()))
                    .build();
            case KNOCKOUT -> resolveKnockout(slot.getId());
        };
    }

    private FootballDataSlotQuery resolveKnockout(String slotId) {
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
            throw new IllegalArgumentException("Unknown playoff stage: " + playoffStage);
        }

        if (leg == null) {
            return FootballDataSlotQuery.builder()
                    .queryType(FootballDataSlotQuery.QueryType.STAGE)
                    .stage(apiStage)
                    .build();
        }
        return FootballDataSlotQuery.builder()
                .queryType(FootballDataSlotQuery.QueryType.STAGE_LEG)
                .stage(apiStage)
                .leg(leg)
                .build();
    }
}
