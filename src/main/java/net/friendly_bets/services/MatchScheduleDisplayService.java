package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.TeamDisplayNamesDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchScheduleDisplayService {

    private static final Set<String> FINISHED_STATUSES = Set.of(
            "FINISHED", "AWARDED", "COMPLETED", "FT", "AET", "PEN"
    );

    private final TeamsRepository teamsRepository;

    public List<ExternalMatchDto> toDisplayDtos(List<MatchSchedule> schedules, String seasonYear) {
        return schedules.stream().map(s -> toDisplayDto(s, seasonYear)).toList();
    }

    public ExternalMatchDto toDisplayDto(MatchSchedule schedule, String seasonYear) {
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
                .build();

        applyTeamDisplay(dto, true, findTeam(schedule.getHomeTeamId()), schedule.getLeagueCode());
        applyTeamDisplay(dto, false, findTeam(schedule.getAwayTeamId()), schedule.getLeagueCode());
        return dto;
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

    public static boolean isFinalized(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getGameScore() != null
                && schedule.getGameScore().getFullTime() != null
                && !schedule.getGameScore().getFullTime().isBlank()) {
            return true;
        }
        String status = schedule.getStatus();
        if (status == null || status.isBlank()) {
            return false;
        }
        return FINISHED_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }
}
