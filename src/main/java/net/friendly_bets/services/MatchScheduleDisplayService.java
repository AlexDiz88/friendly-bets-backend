package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.MatchResultOddsDto;
import net.friendly_bets.dto.TeamDisplayNamesDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchResultOddsExtractor;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchScheduleDisplayService {

    private final TeamsRepository teamsRepository;
    private final OddsRepository oddsRepository;

    public List<ExternalMatchDto> toDisplayDtos(List<MatchSchedule> schedules, String seasonYear) {
        Instant now = Instant.now();
        Map<String, MatchResultOddsDto> oddsByScheduleId = loadMatchResultOdds(schedules, now);
        return schedules.stream()
                .map(s -> toDisplayDto(s, seasonYear, oddsByScheduleId.get(s.getId())))
                .toList();
    }

    public ExternalMatchDto toDisplayDto(MatchSchedule schedule, String seasonYear) {
        MatchResultOddsDto matchResultOdds = null;
        if (schedule != null
                && schedule.getId() != null
                && MatchScheduleNotStarted.isNotStarted(schedule)) {
            matchResultOdds = oddsRepository.findByMatchScheduleId(schedule.getId())
                    .flatMap(MatchResultOddsExtractor::extract)
                    .orElse(null);
        }
        return toDisplayDto(schedule, seasonYear, matchResultOdds);
    }

    private ExternalMatchDto toDisplayDto(
            MatchSchedule schedule,
            String seasonYear,
            MatchResultOddsDto matchResultOdds
    ) {
        boolean finalized = isFinalized(schedule);
        ExternalMatchDto dto = ExternalMatchDto.builder()
                .id(schedule.getId())
                .externalMatchId(0L)
                .leagueCode(schedule.getLeagueCode())
                .matchday(schedule.getMatchday())
                .season(seasonYear)
                .status(schedule.getStatus())
                .utcDate(schedule.getUtcKickoff())
                .homeTeamId(schedule.getHomeTeamId())
                .awayTeamId(schedule.getAwayTeamId())
                .leagueId(schedule.getLeagueId())
                .gameScore(schedule.getGameScore())
                .fetchedAt(schedule.getFetchedAt())
                .finalizedAt(schedule.getFinalizedAt())
                .finalizedSource(schedule.getFinalizedByProvider())
                .finalized(finalized)
                .liveMinuteLabel(schedule.getLiveMinuteLabel())
                .slotId(schedule.getSlotId())
                .adminCorrected(false)
                .goals(schedule.getGoals())
                .addedTimeFirstHalf(schedule.getAddedTimeFirstHalf())
                .addedTimeSecondHalf(schedule.getAddedTimeSecondHalf())
                .stats(schedule.getStats())
                .matchResultOdds(matchResultOdds)
                .build();

        applyTeamDisplay(dto, true, findTeam(schedule.getHomeTeamId()), schedule.getLeagueCode());
        applyTeamDisplay(dto, false, findTeam(schedule.getAwayTeamId()), schedule.getLeagueCode());
        return dto;
    }

    private Map<String, MatchResultOddsDto> loadMatchResultOdds(List<MatchSchedule> schedules, Instant now) {
        Map<String, MatchResultOddsDto> result = new HashMap<>();
        if (schedules == null || schedules.isEmpty()) {
            return result;
        }
        List<String> notStartedIds = schedules.stream()
                .filter(s -> s != null && s.getId() != null && MatchScheduleNotStarted.isNotStarted(s, now))
                .map(MatchSchedule::getId)
                .toList();
        if (notStartedIds.isEmpty()) {
            return result;
        }
        List<Odds> oddsDocs = oddsRepository.findByMatchScheduleIdIn(notStartedIds);
        for (Odds odds : oddsDocs) {
            if (odds == null || odds.getMatchScheduleId() == null) {
                continue;
            }
            MatchResultOddsExtractor.extract(odds)
                    .ifPresent(dto -> result.put(odds.getMatchScheduleId(), dto));
        }
        return result;
    }

    private void applyTeamDisplay(ExternalMatchDto dto, boolean home, Optional<Team> team, String leagueCode) {
        if (team.isEmpty()) {
            return;
        }
        Team t = team.get();
        String country = wcDisplayCountry(leagueCode, t);
        if (home) {
            dto.setHomeTeamTitle(t.getTitle());
            dto.setHomeTeamLogoKey(t.getLogo());
            dto.setHomeTeamDisplayNames(TeamDisplayNamesDto.from(t.getDisplayNames()));
            dto.setHomeTeamCountry(country);
            dto.setHomeTeamName(t.getTitle());
        } else {
            dto.setAwayTeamTitle(t.getTitle());
            dto.setAwayTeamLogoKey(t.getLogo());
            dto.setAwayTeamDisplayNames(TeamDisplayNamesDto.from(t.getDisplayNames()));
            dto.setAwayTeamCountry(country);
            dto.setAwayTeamName(t.getTitle());
        }
    }

    private static String wcDisplayCountry(String leagueCode, Team team) {
        if (!"WC".equals(leagueCode)) {
            return team.getCountry();
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle())
                .or(() -> Optional.ofNullable(team.getCountry()).flatMap(Wc26TeamCatalog::fifaCodeForKnownName))
                .orElse(team.getCountry());
    }

    private Optional<Team> findTeam(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findById(teamId);
    }

    /**
     * FULL_MATCH has locked the result ({@code finalized_at}). Live score / LIVE FINISHED
     * are not enough — otherwise in-play 0:0 looks like «итог зафиксирован».
     */
    public static boolean isFinalized(MatchSchedule schedule) {
        return schedule != null && schedule.getFinalizedAt() != null;
    }
}
