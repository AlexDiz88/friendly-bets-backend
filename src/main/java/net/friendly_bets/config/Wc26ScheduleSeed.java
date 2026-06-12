package net.friendly_bets.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import net.friendly_bets.wc26.Wc26KickoffUtcCalculator;
import net.friendly_bets.wc26.Wc26ScheduleSeedDto;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Wc26ScheduleSeed implements ApplicationRunner {

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Wc26ScheduleSeedDto> entries = loadSeed();
        int inserted = 0;
        for (Wc26ScheduleSeedDto entry : entries) {
            if (wc26ScheduleMatchRepository.existsByScheduleId(entry.getId())) {
                continue;
            }
            wc26ScheduleMatchRepository.save(toEntity(entry));
            inserted++;
        }
        if (inserted > 0) {
            log.info("WC26 schedule seed: inserted {} matches", inserted);
        }
    }

    private List<Wc26ScheduleSeedDto> loadSeed() throws Exception {
        try (InputStream in = new ClassPathResource("wc26/wc26_schedule.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<>() {
            });
        }
    }

    private static Wc26ScheduleMatch toEntity(Wc26ScheduleSeedDto entry) {
        return Wc26ScheduleMatch.builder()
                .scheduleId(entry.getId())
                .date(entry.getDate())
                .timeLocal(entry.getTimeLocal())
                .venueKey(entry.getVenueKey())
                .stage(entry.getStage())
                .group(entry.getGroup())
                .homeFifa(entry.getHome())
                .awayFifa(entry.getAway())
                .labelKey(entry.getLabelKey())
                .kickoffUtc(Wc26KickoffUtcCalculator.kickoffUtc(
                        entry.getDate(),
                        entry.getTimeLocal(),
                        entry.getVenueKey()))
                .build();
    }
}
