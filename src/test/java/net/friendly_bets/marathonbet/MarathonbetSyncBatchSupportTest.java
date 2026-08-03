package net.friendly_bets.marathonbet;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarathonbetSyncBatchSupportTest {

    @Test
    void partitionStages_splitsIntoChunksOfFive() {
        List<MatchSchedule> matches = List.of(
                match("1"), match("2"), match("3"), match("4"), match("5"),
                match("6"), match("7")
        );
        List<List<MatchSchedule>> stages = MarathonbetSyncBatchSupport.partitionStages(matches, 5);
        assertEquals(2, stages.size());
        assertEquals(5, stages.get(0).size());
        assertEquals(2, stages.get(1).size());
    }

    private static MatchSchedule match(String id) {
        return MatchSchedule.builder().id(id).utcKickoff(Instant.parse("2026-08-01T12:00:00Z")).build();
    }
}
