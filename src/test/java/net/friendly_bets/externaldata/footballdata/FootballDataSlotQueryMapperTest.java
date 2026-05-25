package net.friendly_bets.externaldata.footballdata;

import net.friendly_bets.externaldata.ExternalSlotQuery;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FootballDataSlotQueryMapperTest {

    private final FootballDataSlotQueryMapper mapper = new FootballDataSlotQueryMapper();

    @Test
    void mapsClKnockoutPlayOffLegsToPlayoffsBucket() {
        ExpandedMatchdaySlot leg1 = ExpandedMatchdaySlot.builder()
                .id("1/16 [1]")
                .order(9)
                .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                .build();
        ExternalSlotQuery q1 = mapper.map(leg1, "CL").orElseThrow();
        assertEquals(ExternalSlotQuery.QueryType.STAGE_LEG, q1.queryType());
        assertEquals("PLAYOFFS", q1.stage());
        assertEquals(1, q1.leg());

        ExpandedMatchdaySlot leg2 = ExpandedMatchdaySlot.builder()
                .id("1/16 [2]")
                .order(10)
                .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                .build();
        ExternalSlotQuery q2 = mapper.map(leg2, "CL").orElseThrow();
        assertEquals(ExternalSlotQuery.QueryType.STAGE_LEG, q2.queryType());
        assertEquals("PLAYOFFS", q2.stage());
        assertEquals(2, q2.leg());
    }

    @Test
    void mapsLast16WithLegFilter() {
        ExpandedMatchdaySlot slot = ExpandedMatchdaySlot.builder()
                .id("1/8 [2]")
                .order(12)
                .kind(ExpandedMatchdaySlot.Kind.KNOCKOUT)
                .build();
        ExternalSlotQuery query = mapper.map(slot, "CL").orElseThrow();
        assertEquals(ExternalSlotQuery.QueryType.STAGE_LEG, query.queryType());
        assertEquals("LAST_16", query.stage());
        assertEquals(2, query.leg());
    }
}
