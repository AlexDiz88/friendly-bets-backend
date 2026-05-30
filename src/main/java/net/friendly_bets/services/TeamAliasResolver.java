package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TeamAliasResolver {

    private final TeamsRepository teamsRepository;

    public Optional<Team> resolveFootballData(int footballDataTeamId, String footballDataTeamName) {
        // Priority: name from API -> id from API.
        if (footballDataTeamName != null && !footballDataTeamName.isBlank()) {
            Optional<Team> byAliasName = teamsRepository.findByExternalAliasName(
                    TeamTitleUtils.FOOTBALL_DATA_PROVIDER, footballDataTeamName);
            if (byAliasName.isPresent()) {
                return byAliasName;
            }
        }

        Optional<Team> byStoredId = teamsRepository.findByFootballDataTeamId(footballDataTeamId);
        if (byStoredId.isPresent()) {
            return byStoredId;
        }

        Optional<Team> byAliasId = teamsRepository.findByExternalAliasId(
                TeamTitleUtils.FOOTBALL_DATA_PROVIDER, footballDataTeamId);
        if (byAliasId.isPresent()) {
            return byAliasId;
        }

        return Optional.empty();
    }

    public Optional<Team> resolveWc26Code(String wc26Code) {
        if (wc26Code == null || wc26Code.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(TeamTitleUtils.WC26_PROVIDER, wc26Code.trim());
    }

    public Optional<Team> resolveOddsApi(Integer oddsApiTeamId, String oddsApiTeamName) {
        if (oddsApiTeamName != null && !oddsApiTeamName.isBlank()) {
            Optional<Team> byAliasName = teamsRepository.findByExternalAliasName(
                    TeamTitleUtils.ODDS_API_PROVIDER, oddsApiTeamName);
            if (byAliasName.isPresent()) {
                return byAliasName;
            }
        }
        if (oddsApiTeamId != null && oddsApiTeamId > 0) {
            return teamsRepository.findByExternalAliasId(
                    TeamTitleUtils.ODDS_API_PROVIDER, oddsApiTeamId);
        }
        return Optional.empty();
    }

    /**
     * football-data.org team id для внутренней команды (обратное сопоставление к {@link #resolveFootballData}).
     */
    public Optional<Integer> resolveFootballDataTeamId(Team team) {
        if (team == null) {
            return Optional.empty();
        }
        if (team.getFootballDataTeamId() != null && team.getFootballDataTeamId() > 0) {
            return Optional.of(team.getFootballDataTeamId());
        }
        if (team.getExternalAliases() != null) {
            for (TeamExternalAlias alias : team.getExternalAliases()) {
                if (TeamTitleUtils.FOOTBALL_DATA_PROVIDER.equals(alias.getProvider())
                        && alias.getExternalId() != null
                        && alias.getExternalId() > 0) {
                    return Optional.of(alias.getExternalId());
                }
            }
        }
        return Optional.empty();
    }

    public boolean teamMatchesFootballDataSide(Team team, int footballDataTeamId, String footballDataTeamName) {
        if (team == null) {
            return false;
        }
        if (footballDataTeamName != null && !footballDataTeamName.isBlank()
                && team.getExternalAliases() != null) {
            for (TeamExternalAlias alias : team.getExternalAliases()) {
                if (TeamTitleUtils.FOOTBALL_DATA_PROVIDER.equals(alias.getProvider())
                        && footballDataTeamName.equals(alias.getExternalName())) {
                    return true;
                }
            }
        }
        return resolveFootballDataTeamId(team)
                .map(teamFdId -> teamFdId.equals(footballDataTeamId))
                .orElse(false);
    }
}
