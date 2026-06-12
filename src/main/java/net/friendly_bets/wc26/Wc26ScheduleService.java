package net.friendly_bets.wc26;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26ScheduleMatchDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.footballdata.ExternalMatchScoreView;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.Wc26ScheduleMatchRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Wc26ScheduleService {

    private static final String WC_LEAGUE_CODE = "WC";

    private final Wc26ScheduleMatchRepository wc26ScheduleMatchRepository;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final FootballDataProperties footballDataProperties;

    public Wc26SchedulePageDto getSchedulePage(String season) {
        String resolvedSeason = season != null && !season.isBlank()
                ? season
                : footballDataProperties.getDefaultSeason();
        List<Wc26ScheduleMatch> schedule = wc26ScheduleMatchRepository.findAllByOrderByKickoffUtcAsc();
        Map<Integer, GameResultRecord> byScheduleId = indexResultsByScheduleId(resolvedSeason);
        Map<String, GameResultRecord> byPair = indexResultsByPair(resolvedSeason);

        List<Wc26ScheduleMatchDto> matches = schedule.stream()
                .map(entry -> toDto(entry, resolveResult(entry, byScheduleId, byPair)))
                .toList();
        return Wc26SchedulePageDto.builder().matches(matches).build();
    }

    private Map<Integer, GameResultRecord> indexResultsByScheduleId(String season) {
        Map<Integer, GameResultRecord> map = new HashMap<>();
        for (GameResultRecord record : gameResultRecordRepository.findByLeagueCodeAndSeason(WC_LEAGUE_CODE, season)) {
            if (record.getWc26ScheduleId() != null) {
                map.putIfAbsent(record.getWc26ScheduleId(), record);
            }
        }
        return map;
    }

    private Map<String, GameResultRecord> indexResultsByPair(String season) {
        Map<String, GameResultRecord> map = new HashMap<>();
        for (GameResultRecord record : gameResultRecordRepository.findByLeagueCodeAndSeason(WC_LEAGUE_CODE, season)) {
            GameResultSourceSnapshot source = record.footballDataSource();
            if (source == null) {
                continue;
            }
            String homeFifa = fifaFromSide(source.getHome());
            String awayFifa = fifaFromSide(source.getAway());
            if (homeFifa == null || awayFifa == null) {
                continue;
            }
            map.putIfAbsent(pairKey(homeFifa, awayFifa), record);
        }
        return map;
    }

    private static GameResultRecord resolveResult(
            Wc26ScheduleMatch entry,
            Map<Integer, GameResultRecord> byScheduleId,
            Map<String, GameResultRecord> byPair
    ) {
        GameResultRecord byId = byScheduleId.get(entry.getScheduleId());
        if (byId != null) {
            return byId;
        }
        if (entry.getHomeFifa() == null || entry.getAwayFifa() == null) {
            return null;
        }
        return byPair.get(pairKey(entry.getHomeFifa(), entry.getAwayFifa()));
    }

    private static Wc26ScheduleMatchDto toDto(Wc26ScheduleMatch entry, GameResultRecord result) {
        Wc26ScheduleMatchDto.Wc26ScheduleMatchDtoBuilder builder = Wc26ScheduleMatchDto.builder()
                .id(entry.getScheduleId())
                .date(entry.getDate())
                .timeLocal(entry.getTimeLocal())
                .venueKey(entry.getVenueKey())
                .stage(entry.getStage())
                .group(entry.getGroup())
                .home(entry.getHomeFifa())
                .away(entry.getAwayFifa())
                .labelKey(entry.getLabelKey())
                .kickoffUtc(entry.getKickoffUtc())
                .scoreView("—");

        if (result != null) {
            builder.status(result.getStatus())
                    .finalized(result.isFinalized())
                    .utcDate(result.getUtcDate())
                    .scoreView(ExternalMatchScoreView.format(
                            result.getGameScore(),
                            result.getStatus(),
                            result.isFinalized()));
        }
        return builder.build();
    }

    private static String pairKey(String homeFifa, String awayFifa) {
        return homeFifa.toUpperCase() + "|" + awayFifa.toUpperCase();
    }

    private static String fifaFromSide(GameResultSideSnapshot side) {
        if (side == null) {
            return null;
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(side.getExternalName()).orElse(null);
    }
}
