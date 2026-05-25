package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.TeamDisplayNamesDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Обогащает матчи football-data внутренним {@link Team#getTitle()} для UI (логотип, i18n).
 */
@Service
@RequiredArgsConstructor
public class ExternalMatchDisplayService {

    private final TeamsRepository teamsRepository;
    private final TeamAliasResolver teamAliasResolver;

    public List<ExternalMatchDto> toDisplayDtos(List<ExternalMatch> matches) {
        return matches.stream().map(this::toDisplayDto).toList();
    }

    public ExternalMatchDto toDisplayDto(ExternalMatch match) {
        ExternalMatchDto dto = ExternalMatchDto.from(match);
        applyTeamDisplay(dto, true, findTeam(
                match.getHomeTeamId(),
                match.getHomeFootballDataTeamId(),
                match.getHomeTeamName()
        ));
        applyTeamDisplay(dto, false, findTeam(
                match.getAwayTeamId(),
                match.getAwayFootballDataTeamId(),
                match.getAwayTeamName()
        ));
        return dto;
    }

    private void applyTeamDisplay(ExternalMatchDto dto, boolean home, Optional<Team> team) {
        if (team.isEmpty()) {
            return;
        }
        Team t = team.get();
        if (home) {
            dto.setHomeTeamTitle(t.getTitle());
            dto.setHomeTeamLogoKey(t.getLogo());
            dto.setHomeTeamDisplayNames(TeamDisplayNamesDto.from(t.getDisplayNames()));
        } else {
            dto.setAwayTeamTitle(t.getTitle());
            dto.setAwayTeamLogoKey(t.getLogo());
            dto.setAwayTeamDisplayNames(TeamDisplayNamesDto.from(t.getDisplayNames()));
        }
    }

    private Optional<Team> findTeam(String teamId, int footballDataTeamId, String footballDataTeamName) {
        if (teamId != null && !teamId.isBlank()) {
            Optional<Team> byId = teamsRepository.findById(teamId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        return teamAliasResolver.resolveFootballData(footballDataTeamId, footballDataTeamName);
    }
}
