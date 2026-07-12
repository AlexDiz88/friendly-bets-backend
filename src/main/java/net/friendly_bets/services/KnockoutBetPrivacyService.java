package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.utils.KnockoutBetPrivacyStages;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnockoutBetPrivacyService {

    GameResultRecordRepository gameResultRecordRepository;
    MatchdaySlotSupport matchdaySupport;

    public record MatchKey(
            String leagueCode,
            String storageSeason,
            String homeTeamId,
            String awayTeamId,
            String matchDay
    ) {
    }

    public BetDto toDto(Bet bet, String viewerUserId) {
        return toDto(bet, viewerUserId, new HashMap<>());
    }

    public BetDto toDto(Bet bet, String viewerUserId, Map<MatchKey, Boolean> notStartedCache) {
        BetDto dto = BetDto.from(bet);
        if (shouldHideBetDetails(bet, viewerUserId, notStartedCache)) {
            maskDetails(dto);
        }
        return dto;
    }

    public List<BetDto> toDtoList(List<Bet> bets, String viewerUserId) {
        Map<MatchKey, Boolean> notStartedCache = new HashMap<>();
        return bets.stream()
                .map(bet -> toDto(bet, viewerUserId, notStartedCache))
                .collect(Collectors.toList());
    }

    public boolean shouldHideBetDetails(Bet bet, String viewerUserId) {
        return shouldHideBetDetails(bet, viewerUserId, new HashMap<>());
    }

    public boolean shouldHideBetDetails(Bet bet, String viewerUserId, Map<MatchKey, Boolean> notStartedCache) {
        if (bet == null || bet.getBetStatus() != Bet.BetStatus.OPENED || bet.getBetTitle() == null) {
            return false;
        }
        League league = bet.getLeague();
        if (league == null || league.getLeagueCode() == null) {
            return false;
        }
        if (!KnockoutBetPrivacyStages.isSensitiveKnockoutSlot(league.getLeagueCode(), bet.getMatchDay())) {
            return false;
        }
        if (viewerUserId != null && bet.getUser() != null && viewerUserId.equals(bet.getUser().getId())) {
            return false;
        }
        return isMatchNotStarted(bet, notStartedCache);
    }

    public boolean isMatchNotStarted(Bet bet, Map<MatchKey, Boolean> notStartedCache) {
        if (bet.getHomeTeam() == null || bet.getAwayTeam() == null || bet.getSeason() == null) {
            return true;
        }
        String homeTeamId = bet.getHomeTeam().getId();
        String awayTeamId = bet.getAwayTeam().getId();
        if (homeTeamId == null || homeTeamId.isBlank() || awayTeamId == null || awayTeamId.isBlank()) {
            return true;
        }
        League league = bet.getLeague();
        if (league == null || league.getLeagueCode() == null) {
            return true;
        }
        Season season = bet.getSeason();
        String storageSeason = matchdaySupport.resolveExternalSeasonYear(season, league.getLeagueCode());
        MatchKey key = new MatchKey(
                league.getLeagueCode().name(),
                storageSeason,
                homeTeamId,
                awayTeamId,
                bet.getMatchDay()
        );
        return notStartedCache.computeIfAbsent(key, k -> resolveMatchNotStarted(bet, k));
    }

    private boolean resolveMatchNotStarted(Bet bet, MatchKey key) {
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndSeasonAndHomeTeamIdAndAwayTeamId(
                key.leagueCode(),
                key.storageSeason(),
                key.homeTeamId(),
                key.awayTeamId()
        );
        if (matches.isEmpty()) {
            return true;
        }
        League league = bet.getLeague();
        Optional<Integer> slotOrder = matchdaySupport.resolveSlotOrder(league, key.matchDay());
        Optional<GameResultRecord> match = matches.stream()
                .filter(record -> slotOrder.isEmpty() || Objects.equals(record.getMatchday(), slotOrder.get()))
                .findFirst();
        if (match.isEmpty()) {
            return true;
        }
        return GameResultNotStarted.isNotStarted(match.get());
    }

    private static BetDto maskDetails(BetDto dto) {
        dto.setBetTitle(null);
        dto.setBetOdds(null);
        dto.setBetSize(null);
        dto.setBetDetailsHidden(true);
        return dto;
    }
}
