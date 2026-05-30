package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.dto.Wc26BettingContextDto;
import net.friendly_bets.dto.Wc26GameResultLookupDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.User;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import net.friendly_bets.services.GetEntityService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Wc26MatchService {

    private final SeasonsRepository seasonsRepository;
    private final TournamentFormatsRepository tournamentFormatsRepository;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final Wc26GameResultLinker gameResultLinker;
    private final GetEntityService getEntityService;

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

    public Wc26GameResultLookupDto lookupGameResult(String seasonId, int wc26ScheduleId, String slotId) {
        if (!Wc26ScheduleCatalog.isGroupStage(wc26ScheduleId)) {
            throw new BadRequestException("wc26ScheduleIdNotSupported");
        }
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        String storageSeason = matchdaySupport.resolveFootballDataSeasonYear(season, League.LeagueCode.WC);
        GameResultRecord record = gameResultLinker.findByScheduleId(wc26ScheduleId, storageSeason)
                .orElseThrow(() -> new BadRequestException("wc26GameResultNotMapped"));
        return Wc26GameResultLookupDto.builder()
                .gameResultId(record.getId())
                .wc26ScheduleId(wc26ScheduleId)
                .homeTeamId(record.getHomeTeamId())
                .awayTeamId(record.getAwayTeamId())
                .kickoffUtc(record.getUtcDate())
                .slotId(slotId)
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
