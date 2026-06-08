package net.friendly_bets.oddsapi;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.models.odds.GameResultMergedOdds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetMergedOddsGuardTest {

    @Test
    void hasProductionMarathonOdds_trueWhenMarathonBookmakerAndGroupsPresent() {
        GameResultMergedOdds doc = GameResultMergedOdds.builder()
                .gameResultId("gr1")
                .bookmakers(List.of(MarathonbetBookmaker.KEY))
                .marketGroups(List.of(OddsMarketGroup.builder().category("RESULT").build()))
                .build();

        assertTrue(MarathonbetMergedOddsGuard.hasProductionMarathonOdds(Optional.of(doc)));
    }

    @Test
    void hasProductionMarathonOdds_falseWhenEmptyOrMissingMarathon() {
        assertFalse(MarathonbetMergedOddsGuard.hasProductionMarathonOdds(Optional.empty()));

        GameResultMergedOdds noGroups = GameResultMergedOdds.builder()
                .bookmakers(List.of(MarathonbetBookmaker.KEY))
                .marketGroups(List.of())
                .build();
        assertFalse(MarathonbetMergedOddsGuard.hasProductionMarathonOdds(Optional.of(noGroups)));

        GameResultMergedOdds oddsApiOnly = GameResultMergedOdds.builder()
                .bookmakers(List.of("Bet365"))
                .marketGroups(List.of(OddsMarketGroup.builder().category("RESULT").build()))
                .build();
        assertFalse(MarathonbetMergedOddsGuard.hasProductionMarathonOdds(Optional.of(oddsApiOnly)));
    }
}
