package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.Wc26BettingContextDto;
import net.friendly_bets.dto.Wc26BettingSlotDto;
import net.friendly_bets.dto.Wc26GroupStageBoardDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.GameResultDisplayService;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.User;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TournamentFormatExpander;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class Wc26MatchService {

    private static final Pattern GROUP_SLOT_ID = Pattern.compile("^r([123])-s(\\d+)$");

    private final SeasonsRepository seasonsRepository;
    private final TournamentFormatsRepository tournamentFormatsRepository;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final GetEntityService getEntityService;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultDisplayService gameResultDisplayService;
    private final TournamentFormatExpander tournamentFormatExpander;

    public Wc26BettingContextDto getBettingContext(String userId) {
        Optional<Season> active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (active.isEmpty()) {
            return Wc26BettingContextDto.builder().bettingEnabled(false).seasonParticipant(false).build();
        }
        Season season = active.get();
        Optional<League> wcLeague = season.getLeagues().stream()
                .filter(l -> l != null && l.getLeagueCode() == League.LeagueCode.WC)
                .findFirst();
        if (wcLeague.isEmpty() || wcLeague.get().getTournamentFormatId() == null
                || wcLeague.get().getTournamentFormatId().isBlank()) {
            return Wc26BettingContextDto.builder()
                    .bettingEnabled(false)
                    .seasonId(season.getId())
                    .seasonParticipant(isParticipant(season, userId))
                    .build();
        }
        TournamentFormat format = tournamentFormatsRepository.findById(wcLeague.get().getTournamentFormatId())
                .orElse(null);
        boolean wcFormat = format != null && WcTournamentSlots.FORMAT_CODE.equals(format.getFormatCode());
        return Wc26BettingContextDto.builder()
                .bettingEnabled(wcFormat)
                .seasonId(season.getId())
                .leagueId(wcLeague.get().getId())
                .leagueCode(League.LeagueCode.WC.name())
                .tournamentFormatId(wcLeague.get().getTournamentFormatId())
                .seasonParticipant(isParticipant(season, userId))
                .build();
    }

    /**
     * Group-stage betting board from {@code game_results} (same source as «Результаты матчей»).
     * Each slot {@code r1-s1}…{@code r3-s4} maps to {@link GameResultRecord#getMatchday()} = tournament slot order.
     */
    public Wc26GroupStageBoardDto getGroupStageBoard(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        League wcLeague = requireWcLeague(season);
        TournamentFormat format = tournamentFormatsRepository.findById(wcLeague.getTournamentFormatId())
                .orElseThrow(() -> new BadRequestException("tournamentFormatNotFound"));
        if (!WcTournamentSlots.FORMAT_CODE.equals(format.getFormatCode())) {
            throw new BadRequestException("wcBettingFormatRequired");
        }
        String storageSeason = matchdaySupport.resolveFootballDataSeasonYear(season, League.LeagueCode.WC);
        List<ExpandedMatchdaySlot> allSlots = tournamentFormatExpander.expand(format);
        List<Wc26BettingSlotDto> slots = new ArrayList<>();
        for (ExpandedMatchdaySlot expanded : allSlots) {
            if (expanded.getOrder() < 1 || expanded.getOrder() > WcTournamentSlots.GROUP_SLOT_COUNT) {
                continue;
            }
            String slotId = expanded.getId();
            Matcher matcher = GROUP_SLOT_ID.matcher(slotId != null ? slotId : "");
            if (!matcher.matches()) {
                continue;
            }
            int round = Integer.parseInt(matcher.group(1));
            int slotIndex = Integer.parseInt(matcher.group(2));
            int matchesPerSlot = round == 3 ? 6 : 4;
            List<GameResultRecord> records = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                    League.LeagueCode.WC.name(),
                    expanded.getOrder(),
                    storageSeason
            );
            records.sort(Comparator.comparing(
                    GameResultRecord::getUtcDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
            List<ExternalMatchDto> matches = gameResultDisplayService.toDisplayDtos(records);
            slots.add(Wc26BettingSlotDto.builder()
                    .id(slotId)
                    .round(round)
                    .slotIndex(slotIndex)
                    .betsRequired(WcTournamentSlots.betsRequiredForSlot(slotId))
                    .matchesPerSlot(matchesPerSlot)
                    .matches(matches)
                    .build());
        }
        return Wc26GroupStageBoardDto.builder()
                .seasonId(seasonId)
                .leagueId(wcLeague.getId())
                .storageSeason(storageSeason)
                .slots(slots)
                .build();
    }

    public League requireWcLeague(Season season) {
        return season.getLeagues().stream()
                .filter(l -> l != null && l.getLeagueCode() == League.LeagueCode.WC)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("wcLeagueNotInSeason"));
    }

    private static boolean isParticipant(Season season, String userId) {
        if (userId == null || season.getPlayers() == null) {
            return false;
        }
        return season.getPlayers().stream()
                .filter(Objects::nonNull)
                .map(User::getId)
                .anyMatch(userId::equals);
    }
}
