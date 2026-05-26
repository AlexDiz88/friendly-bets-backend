package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.BetsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutoBetSettlementService {

    private static final Logger log = LoggerFactory.getLogger(AutoBetSettlementService.class);

    private final FootballDataProperties properties;
    private final SeasonsRepository seasonsRepository;
    private final UsersRepository usersRepository;
    private final ExternalMatchGameResultCollector gameResultCollector;
    private final BetsService betsService;

    public Optional<AutoSettleResult> settleActiveSeasonIfEnabled() {
        if (!properties.isAutoSettleEnabled()) {
            return Optional.empty();
        }
        return seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE)
                .map(this::settleSeason);
    }

    @Transactional
    public AutoSettleResult settleSeason(Season season) {
        List<GameResult> gameResults = gameResultCollector.collectForSeason(season);
        if (gameResults.isEmpty()) {
            log.info("Auto-settle: no settleable matches for season {}", season.getId());
            return AutoSettleResult.builder()
                    .seasonId(season.getId())
                    .matchesSubmitted(0)
                    .betsProcessed(0)
                    .executed(false)
                    .build();
        }

        String moderatorId = resolveSystemModeratorId();
        BetsPage page = betsService.setBetResults(moderatorId, season.getId(), gameResults);
        int betsProcessed = page.getBets() != null ? page.getBets().size() : 0;

        log.info(
                "Auto-settle season {}: {} match(es), {} bet(s) processed",
                season.getId(),
                gameResults.size(),
                betsProcessed
        );

        return AutoSettleResult.builder()
                .seasonId(season.getId())
                .matchesSubmitted(gameResults.size())
                .betsProcessed(betsProcessed)
                .executed(true)
                .build();
    }

    private String resolveSystemModeratorId() {
        if (properties.getSystemModeratorId() != null && !properties.getSystemModeratorId().isBlank()) {
            return properties.getSystemModeratorId().trim();
        }
        return usersRepository.findFirstByRoleOrderByCreatedAtAsc(User.Role.ADMIN)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "football-data.system-moderator-id is empty and no ADMIN user found"));
    }
}
