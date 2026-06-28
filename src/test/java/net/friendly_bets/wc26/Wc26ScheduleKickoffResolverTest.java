package net.friendly_bets.wc26;

import net.friendly_bets.fourscore.FourScoreListDates;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Wc26ScheduleKickoffResolverTest {

    @Mock
    private Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;

    @InjectMocks
    private Wc26ScheduleKickoffResolver resolver;

    @BeforeEach
    void clearKickoffLookup() {
        Wc26ScheduleKickoffLookup.install(Map.of());
    }

    @Test
    void kickoffUtc_roundOf32_mexicoCitySlot_usesBerlinTime() {
        Wc26ScheduleMatch match = Wc26ScheduleMatch.builder()
                .scheduleId(79)
                .date("2026-07-01")
                .timeLocal("03:00")
                .venueKey("mexicoCity")
                .stage("round_of_32")
                .build();
        when(wc26ScheduleMatchRepository.findByScheduleId(79)).thenReturn(Optional.of(match));

        LocalDateTime kickoff = resolver.kickoffUtc(79).orElseThrow();

        assertEquals(LocalDateTime.of(2026, 7, 1, 1, 0), kickoff);
    }

    @Test
    void kickoffUtc_torontoRoundOf32_listPageIsMoscowJuly3() {
        Wc26ScheduleMatch match = Wc26ScheduleMatch.builder()
                .scheduleId(84)
                .date("2026-07-02")
                .timeLocal("23:00")
                .venueKey("toronto")
                .stage("round_of_32")
                .build();
        when(wc26ScheduleMatchRepository.findByScheduleId(84)).thenReturn(Optional.of(match));

        LocalDateTime kickoff = resolver.kickoffUtc(84).orElseThrow();
        assertEquals(LocalDate.of(2026, 7, 3), FourScoreListDates.listPageDateFromKickoffUtc(kickoff));
    }
}
