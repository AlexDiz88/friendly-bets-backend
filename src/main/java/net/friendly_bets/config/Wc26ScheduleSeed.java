package net.friendly_bets.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import net.friendly_bets.wc26.Wc26BerlinKickoffCalculator;
import net.friendly_bets.wc26.Wc26KickoffUtcCalculator;
import net.friendly_bets.wc26.Wc26ScheduleCatalog;
import net.friendly_bets.wc26.Wc26ScheduleSeedDto;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class Wc26ScheduleSeed implements ApplicationRunner {

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Wc26ScheduleSeedDto> entries = loadSeed();
        int inserted = 0;
        int kickoffBackfilled = 0;
        int teamsBackfilled = 0;
        int playoffRefreshed = 0;
        for (Wc26ScheduleSeedDto entry : entries) {
            if (wc26ScheduleMatchRepository.existsByScheduleId(entry.getId())) {
                kickoffBackfilled += backfillKickoffUtcIfMissing(entry);
                teamsBackfilled += backfillTeamsFromCatalogIfMissing(entry);
                playoffRefreshed += refreshPlayoffIfNeeded(entry);
                continue;
            }
            Wc26ScheduleMatch entity = toEntity(entry);
            applyCatalogTeamsIfMissing(entity);
            wc26ScheduleMatchRepository.save(entity);
            inserted++;
        }
        if (inserted > 0) {
            log.info("WC26 schedule seed: inserted {} matches", inserted);
        }
        if (kickoffBackfilled > 0) {
            log.info("WC26 schedule seed: backfilled kickoffUtc for {} matches", kickoffBackfilled);
        }
        if (teamsBackfilled > 0) {
            log.info("WC26 schedule seed: backfilled home/away FIFA for {} matches", teamsBackfilled);
        }
        if (playoffRefreshed > 0) {
            log.info("WC26 schedule seed: refreshed FIFA playoff schedule for {} matches", playoffRefreshed);
        }
    }

    private int refreshPlayoffIfNeeded(Wc26ScheduleSeedDto entry) {
        if (!Wc26BerlinKickoffCalculator.isPlayoffStage(entry.getStage())) {
            return 0;
        }
        return wc26ScheduleMatchRepository.findByScheduleId(entry.getId())
                .map(match -> {
                    boolean changed = false;
                    if (!entry.getDate().equals(match.getDate())) {
                        match.setDate(entry.getDate());
                        changed = true;
                    }
                    if (!entry.getTimeLocal().equals(match.getTimeLocal())) {
                        match.setTimeLocal(entry.getTimeLocal());
                        changed = true;
                    }
                    if (entry.getVenueKey() != null && !entry.getVenueKey().equals(match.getVenueKey())) {
                        match.setVenueKey(entry.getVenueKey());
                        changed = true;
                    }
                    if (entry.getLabelKey() != null && !entry.getLabelKey().equals(match.getLabelKey())) {
                        match.setLabelKey(entry.getLabelKey());
                        changed = true;
                    }
                    if (entry.getHome() != null && !entry.getHome().equals(match.getHomeFifa())) {
                        match.setHomeFifa(entry.getHome());
                        changed = true;
                    }
                    if (entry.getAway() != null && !entry.getAway().equals(match.getAwayFifa())) {
                        match.setAwayFifa(entry.getAway());
                        changed = true;
                    }
                    LocalDateTime kickoff = computeKickoffUtc(entry);
                    if (kickoff != null && !kickoff.equals(match.getKickoffUtc())) {
                        match.setKickoffUtc(kickoff);
                        changed = true;
                    }
                    if (changed) {
                        wc26ScheduleMatchRepository.save(match);
                        return 1;
                    }
                    return 0;
                })
                .orElse(0);
    }

    private int backfillTeamsFromCatalogIfMissing(Wc26ScheduleSeedDto entry) {
        if (Wc26BerlinKickoffCalculator.isPlayoffStage(entry.getStage())) {
            return wc26ScheduleMatchRepository.findByScheduleId(entry.getId())
                    .filter(match -> entry.getHome() != null && entry.getAway() != null
                            && (match.getHomeFifa() == null || match.getAwayFifa() == null))
                    .map(match -> {
                        match.setHomeFifa(entry.getHome());
                        match.setAwayFifa(entry.getAway());
                        wc26ScheduleMatchRepository.save(match);
                        return 1;
                    })
                    .orElse(0);
        }
        return wc26ScheduleMatchRepository.findByScheduleId(entry.getId())
                .filter(match -> match.getHomeFifa() == null || match.getAwayFifa() == null)
                .map(match -> {
                    applyCatalogTeamsIfMissing(match);
                    if (match.getHomeFifa() != null && match.getAwayFifa() != null) {
                        wc26ScheduleMatchRepository.save(match);
                        return 1;
                    }
                    return 0;
                })
                .orElse(0);
    }

    private static void applyCatalogTeamsIfMissing(Wc26ScheduleMatch match) {
        if (match == null || Wc26BerlinKickoffCalculator.isPlayoffStage(match.getStage())) {
            return;
        }
        Wc26ScheduleCatalog.findById(match.getScheduleId()).ifPresent(catalog -> {
            if (match.getHomeFifa() == null) {
                match.setHomeFifa(catalog.homeFifa());
            }
            if (match.getAwayFifa() == null) {
                match.setAwayFifa(catalog.awayFifa());
            }
        });
    }

    private int backfillKickoffUtcIfMissing(Wc26ScheduleSeedDto entry) {
        return wc26ScheduleMatchRepository.findByScheduleId(entry.getId())
                .filter(match -> match.getKickoffUtc() == null)
                .map(match -> {
                    match.setKickoffUtc(computeKickoffUtc(entry));
                    wc26ScheduleMatchRepository.save(match);
                    return 1;
                })
                .orElse(0);
    }

    private List<Wc26ScheduleSeedDto> loadSeed() throws Exception {
        try (InputStream in = new ClassPathResource("wc26/wc26_schedule.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<>() {
            });
        }
    }

    private static Wc26ScheduleMatch toEntity(Wc26ScheduleSeedDto entry) {
        Wc26ScheduleMatch match = Wc26ScheduleMatch.builder()
                .scheduleId(entry.getId())
                .date(entry.getDate())
                .timeLocal(entry.getTimeLocal())
                .venueKey(entry.getVenueKey())
                .stage(entry.getStage())
                .group(entry.getGroup())
                .homeFifa(entry.getHome())
                .awayFifa(entry.getAway())
                .labelKey(entry.getLabelKey())
                .kickoffUtc(computeKickoffUtc(entry))
                .build();
        applyCatalogTeamsIfMissing(match);
        return match;
    }

    private static LocalDateTime computeKickoffUtc(Wc26ScheduleSeedDto entry) {
        return Wc26KickoffUtcCalculator.kickoffUtc(
                entry.getDate(),
                entry.getTimeLocal(),
                entry.getVenueKey(),
                entry.getStage());
    }
}
