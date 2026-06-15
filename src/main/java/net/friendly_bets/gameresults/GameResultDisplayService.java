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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameResultDisplayService {

    private final TeamsRepository teamsRepository;
    private final TeamAliasResolver teamAliasResolver;

    public List<ExternalMatchDto> toDisplayDtos(List<GameResultRecord> matches) {
        return matches.stream().map(this::toDisplayDto).toList();
    }

    public ExternalMatchDto toDisplayDto(GameResultRecord match) {
        ExternalMatchDto dto = ExternalMatchDto.from(match);
        GameResultSourceSnapshot source = match.primaryExternalSource();
        String provider = match.getProvider() != null ? match.getProvider() : MatchDataProviders.FOURSCORE;
        applyTeamDisplay(
                dto,
                true,
                findTeam(match.getHomeTeamId(), provider, externalName(source != null ? source.getHome() : null)));
        applyTeamDisplay(
                dto,
                false,
                findTeam(match.getAwayTeamId(), provider, externalName(source != null ? source.getAway() : null)));
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
            dto.setHomeTeamCountry(t.getCountry());
            dto.setHomeTeamName(t.getTitle());
        } else {
            dto.setAwayTeamTitle(t.getTitle());
            dto.setAwayTeamLogoKey(t.getLogo());
            dto.setAwayTeamDisplayNames(TeamDisplayNamesDto.from(t.getDisplayNames()));
            dto.setAwayTeamCountry(t.getCountry());
            dto.setAwayTeamName(t.getTitle());
        }
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
