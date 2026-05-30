package net.friendly_bets.config;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.PlayoffRound;
import net.friendly_bets.models.RoundRobinStage;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Replaces legacy wc-48teams structure (3 group matchdays) with Berlin 16 slots + expanded playoff.
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class WcTournamentFormatMigration implements ApplicationRunner {

    private final TournamentFormatsRepository tournamentFormatsRepository;

    @Override
    public void run(ApplicationArguments args) {
        Optional<TournamentFormat> existing = tournamentFormatsRepository.findByFormatCode(WcTournamentSlots.FORMAT_CODE);
        if (existing.isEmpty()) {
            return;
        }
        TournamentFormat format = existing.get();
        if (isAlreadyMigrated(format)) {
            return;
        }
        format.setGroupStage(RoundRobinStage.builder().matchdayCount(3).build());
        format.setPlayoff(List.of(
                PlayoffRound.builder().stage("1/16").matchdayCount(5).build(),
                PlayoffRound.builder().stage("1/8").matchdayCount(4).build(),
                PlayoffRound.builder().stage("1/4").matchdayCount(2).build(),
                PlayoffRound.builder().stage("1/2").matchdayCount(1).build(),
                PlayoffRound.builder().stage("third_place").matchdayCount(1).build(),
                PlayoffRound.builder().stage("final").matchdayCount(1).build()
        ));
        tournamentFormatsRepository.save(format);
    }

    private static boolean isAlreadyMigrated(TournamentFormat format) {
        if (format.getPlayoff() == null || format.getPlayoff().isEmpty()) {
            return false;
        }
        return format.getPlayoff().stream()
                .anyMatch(p -> "1/16".equals(p.getStage()) && p.getMatchdayCount() >= 5);
    }
}
