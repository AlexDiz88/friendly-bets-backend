package net.friendly_bets.config;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.PlayoffRound;
import net.friendly_bets.models.RoundRobinStage;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TournamentFormatSeed implements ApplicationRunner {

    private final TournamentFormatsRepository tournamentFormatsRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("epl-standard", "АПЛ - 38 туров", epl());
        seedIfMissing("bl-standard", "Бундеслига - 34 тура", bl());
        seedIfMissing("cl-newFormat", "Лига чемпионов - обновлённый формат с общей группой", cl());
        seedIfMissing("wc-48teams", "ЧМ - 48 команд", wc());
    }

    private void seedIfMissing(String code, String name, TournamentFormat format) {
        if (tournamentFormatsRepository.existsByFormatCode(code)) {
            return;
        }
        format.setFormatCode(code);
        format.setName(name);
        format.setCreatedAt(LocalDateTime.now());
        tournamentFormatsRepository.save(format);
    }

    private static TournamentFormat epl() {
        return TournamentFormat.builder()
                .regularStage(RoundRobinStage.builder().matchdayCount(38).build())
                .build();
    }

    private static TournamentFormat bl() {
        return TournamentFormat.builder()
                .regularStage(RoundRobinStage.builder().matchdayCount(34).build())
                .build();
    }

    private static TournamentFormat cl() {
        return TournamentFormat.builder()
                .groupStage(RoundRobinStage.builder().matchdayCount(8).build())
                .playoff(List.of(
                        PlayoffRound.builder().stage("1/16").matchdayCount(2).build(),
                        PlayoffRound.builder().stage("1/8").matchdayCount(2).build(),
                        PlayoffRound.builder().stage("1/4").matchdayCount(2).build(),
                        PlayoffRound.builder().stage("1/2").matchdayCount(2).build(),
                        PlayoffRound.builder().stage("final").matchdayCount(1).build()
                ))
                .build();
    }

    private static TournamentFormat wc() {
        return TournamentFormat.builder()
                .groupStage(RoundRobinStage.builder().matchdayCount(3).build())
                .playoff(List.of(
                        PlayoffRound.builder().stage("1/16").matchdayCount(1).build(),
                        PlayoffRound.builder().stage("1/8").matchdayCount(1).build(),
                        PlayoffRound.builder().stage("1/4").matchdayCount(1).build(),
                        PlayoffRound.builder().stage("1/2").matchdayCount(1).build(),
                        PlayoffRound.builder().stage("third_place").matchdayCount(1).build(),
                        PlayoffRound.builder().stage("final").matchdayCount(1).build()
                ))
                .build();
    }
}
