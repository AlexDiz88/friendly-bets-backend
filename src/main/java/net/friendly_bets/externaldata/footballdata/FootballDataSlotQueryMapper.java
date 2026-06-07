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

    /**
     * football-data.org v4 {@code stage} for filter on {@code /competitions/{code}/matches}.
     * Knockout play-offs before R16: football-data returns {@code PLAYOFFS} bucket; legs split via {@link net.friendly_bets.footballdata.FootballDataLegFilter}.
     *
     * @see <a href="https://docs.football-data.org/general/v4/lookup_tables.html">Match.stage enum</a>
     */
    private static final Map<String, String> PLAYOFF_STAGE_TO_API = Map.ofEntries(
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
    public Optional<ExternalSlotQuery> map(ExpandedMatchdaySlot slot, String competitionCode) {
        if (slot == null || competitionCode == null || competitionCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(switch (slot.getKind()) {
            case REGULAR, GROUP -> ExternalSlotQuery.builder()
                    .queryType(ExternalSlotQuery.QueryType.MATCHDAY)
                    .matchday(FootballDataBettingSlotApiMatchdayResolver.resolveApiMatchday(competitionCode, slot))
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
        if ("1/16".equals(playoffStage)) {
            int resolvedLeg = leg == null ? 1 : leg;
            return ExternalSlotQuery.builder()
                    .queryType(ExternalSlotQuery.QueryType.STAGE_LEG)
                    .stage("PLAYOFFS")
                    .leg(resolvedLeg)
                    .build();
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
