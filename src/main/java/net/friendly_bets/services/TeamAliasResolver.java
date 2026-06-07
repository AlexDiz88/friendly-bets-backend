package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamTitleUtils;
import net.friendly_bets.wc26.Wc26TeamCatalog;
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

        return Wc26TeamCatalog.fifaCodeForKnownName(footballDataTeamName)
                .flatMap(this::resolveWc26Code);
    }

    /**
     * Maps WC26 schedule FIFA code (KOR, CZE, …) to internal team via odds-api.io alias only.
     */
    public Optional<Team> resolveWc26Code(String wc26Code) {
        if (wc26Code == null || wc26Code.isBlank()) {
            return Optional.empty();
        }
        String code = wc26Code.trim();
        for (String oddsApiName : Wc26TeamCatalog.oddsApiNameCandidatesForFifaCode(code)) {
            Optional<Team> byOddsApi = resolveOddsApi(null, oddsApiName);
            if (byOddsApi.isPresent()) {
                return byOddsApi;
            }
        }
        return Optional.empty();
    }

    public boolean oddsApiAliasesMapped(Integer oddsApiTeamId, String oddsApiTeamName) {
        boolean idOk = oddsApiTeamId == null || oddsApiTeamId <= 0 || resolveOddsApiById(oddsApiTeamId).isPresent();
        boolean nameOk = oddsApiTeamName == null || oddsApiTeamName.isBlank()
                || resolveOddsApiByName(oddsApiTeamName).isPresent();
        return idOk && nameOk;
    }

    public Optional<Team> resolveOddsApiById(Integer oddsApiTeamId) {
        if (oddsApiTeamId == null || oddsApiTeamId <= 0) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasId(
                TeamTitleUtils.ODDS_API_PROVIDER, oddsApiTeamId);
    }

    public Optional<Team> resolveOddsApiByName(String oddsApiTeamName) {
        if (oddsApiTeamName == null || oddsApiTeamName.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(
                TeamTitleUtils.ODDS_API_PROVIDER, oddsApiTeamName);
    }

    public Optional<Team> resolveMarathonbetByName(String marathonTeamName) {
        if (marathonTeamName == null || marathonTeamName.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(
                TeamTitleUtils.MARATHONBET_PROVIDER, marathonTeamName.trim());
    }

    public Optional<Team> resolveOddsApi(Integer oddsApiTeamId, String oddsApiTeamName) {
        Optional<Team> byId = resolveOddsApiById(oddsApiTeamId);
        if (byId.isPresent()) {
            return byId;
        }
        return resolveOddsApiByName(oddsApiTeamName);
    }

    public Optional<Team> resolveApiFootball(Integer apiFootballTeamId, String apiFootballTeamName) {
        if (apiFootballTeamName != null && !apiFootballTeamName.isBlank()) {
            Optional<Team> byAliasName = teamsRepository.findByExternalAliasName(
                    TeamTitleUtils.API_FOOTBALL_PROVIDER, apiFootballTeamName);
            if (byAliasName.isPresent()) {
                return byAliasName;
            }
        }
        if (apiFootballTeamId != null && apiFootballTeamId > 0) {
            return teamsRepository.findByExternalAliasId(
                    TeamTitleUtils.API_FOOTBALL_PROVIDER, apiFootballTeamId);
        }
        return Optional.empty();
    }

    public Optional<Integer> resolveApiFootballTeamId(Team team) {
        if (team == null || team.getExternalAliases() == null) {
            return Optional.empty();
        }
        for (TeamExternalAlias alias : team.getExternalAliases()) {
            if (TeamTitleUtils.API_FOOTBALL_PROVIDER.equals(alias.getProvider())
                    && alias.getExternalId() != null
                    && alias.getExternalId() > 0) {
                return Optional.of(alias.getExternalId());
            }
        }
        return Optional.empty();
    }

    public boolean teamMatchesApiFootballSide(Team team, int apiFootballTeamId, String apiFootballTeamName) {
        if (team == null) {
            return false;
        }
        if (apiFootballTeamName != null && !apiFootballTeamName.isBlank()
                && team.getExternalAliases() != null) {
            for (TeamExternalAlias alias : team.getExternalAliases()) {
                if (TeamTitleUtils.API_FOOTBALL_PROVIDER.equals(alias.getProvider())
                        && apiFootballTeamName.equals(alias.getExternalName())) {
                    return true;
                }
            }
        }
        return resolveApiFootballTeamId(team)
                .map(id -> id.equals(apiFootballTeamId))
                .orElse(false);
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
