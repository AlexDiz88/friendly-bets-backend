package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.TeamDisplayNamesDto;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.wc26.Wc26ScheduleKickoffResolver;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameResultDisplayService {

    private final TeamsRepository teamsRepository;
    private final TeamAliasResolver teamAliasResolver;
    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;

    public List<ExternalMatchDto> toDisplayDtos(List<GameResultRecord> matches) {
        return matches.stream().map(this::toDisplayDto).toList();
    }

    public ExternalMatchDto toDisplayDto(GameResultRecord match) {
        ExternalMatchDto dto = ExternalMatchDto.from(match);
        LocalDateTime displayUtc = resolveDisplayUtcDate(match);
        if (displayUtc != null) {
            dto.setUtcDate(displayUtc);
        }
        GameResultSourceSnapshot source = match.primaryExternalSource();
        String provider = match.getProvider() != null ? match.getProvider() : MatchDataProviders.FOURSCORE;
        applyTeamDisplay(
                dto,
                true,
                findTeam(match.getHomeTeamId(), provider, externalName(source != null ? source.getHome() : null)),
                match.getLeagueCode());
        applyTeamDisplay(
                dto,
                false,
                findTeam(match.getAwayTeamId(), provider, externalName(source != null ? source.getAway() : null)),
                match.getLeagueCode());
        return dto;
    }

    /**
     * Для UI: для ЧМ — kickoff из wc26_schedule (venue → UTC), иначе utcDate API.
     */
    private LocalDateTime resolveDisplayUtcDate(GameResultRecord match) {
        if ("WC".equals(match.getLeagueCode())) {
            Optional<LocalDateTime> fromSchedule = resolveWcScheduleKickoffUtc(match);
            if (fromSchedule.isPresent()) {
                return fromSchedule.get();
            }
        }
        if (match.getUtcDate() != null) {
            return match.getUtcDate();
        }
        GameResultSourceSnapshot source = match.primaryExternalSource();
        if (source != null && source.getUtcDate() != null) {
            return source.getUtcDate();
        }
        if ("WC".equals(match.getLeagueCode())) {
            return resolveWcScheduleKickoffUtc(match).orElse(null);
        }
        return null;
    }

    private Optional<LocalDateTime> resolveWcScheduleKickoffUtc(GameResultRecord match) {
        if (match.getWc26ScheduleId() != null) {
            return wc26ScheduleKickoffResolver.kickoffUtc(match.getWc26ScheduleId());
        }
        GameResultSourceSnapshot source = match.primaryExternalSource();
        String homeFifa = fifaFromTeamId(match.getHomeTeamId(), source != null ? source.getHome() : null);
        String awayFifa = fifaFromTeamId(match.getAwayTeamId(), source != null ? source.getAway() : null);
        return wc26ScheduleKickoffResolver.kickoffForTeamPair(homeFifa, awayFifa);
    }

    private String fifaFromTeamId(String teamId, GameResultSideSnapshot sideFallback) {
        if (teamId != null && !teamId.isBlank()) {
            Optional<Team> team = teamsRepository.findById(teamId);
            if (team.isPresent()) {
                return Wc26TeamCatalog.fifaCodeForKnownName(team.get().getTitle())
                        .or(() -> Optional.ofNullable(team.get().getCountry()).flatMap(Wc26TeamCatalog::fifaCodeForKnownName))
                        .orElse(null);
            }
        }
        if (sideFallback != null) {
            return Wc26TeamCatalog.fifaCodeForKnownName(sideFallback.getExternalName()).orElse(null);
        }
        return null;
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

    private Optional<Team> findTeam(String teamId, String provider, String externalTeamName) {
        if (teamId != null && !teamId.isBlank()) {
            Optional<Team> byId = teamsRepository.findById(teamId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (externalTeamName == null || externalTeamName.isBlank()) {
            return Optional.empty();
        }
        if (MatchDataProviders.TWENTYFOUR_SCORE.equals(provider)) {
            return teamAliasResolver.resolveTwentyFourScoreByName(externalTeamName);
        }
        return teamAliasResolver.resolveFourScoreByName(externalTeamName);
    }

    private static String externalName(GameResultSideSnapshot side) {
        return side != null ? side.getExternalName() : null;
    }
}
