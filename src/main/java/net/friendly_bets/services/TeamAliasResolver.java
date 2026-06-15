package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
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

    public Optional<Team> resolveFourScoreByName(String fourScoreTeamName) {
        if (fourScoreTeamName == null || fourScoreTeamName.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(
                TeamTitleUtils.FOURSCORE_PROVIDER, fourScoreTeamName.trim());
    }

    public Optional<Team> resolveTwentyFourScoreByName(String twentyFourScoreTeamName) {
        if (twentyFourScoreTeamName == null || twentyFourScoreTeamName.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(
                TeamTitleUtils.TWENTYFOUR_SCORE_PROVIDER, twentyFourScoreTeamName.trim());
    }

    public Optional<Team> resolveOddsApi(Integer oddsApiTeamId, String oddsApiTeamName) {
        Optional<Team> byId = resolveOddsApiById(oddsApiTeamId);
        if (byId.isPresent()) {
            return byId;
        }
        return resolveOddsApiByName(oddsApiTeamName);
    }

    /** Сопоставление стороны матча с внутренней командой только по alias того же провайдера. */
    public boolean teamMatchesScoreProviderSide(Team team, String provider, String externalTeamName) {
        if (team == null || externalTeamName == null || externalTeamName.isBlank()) {
            return false;
        }
        String resolvedProvider = provider != null ? provider : MatchDataProviders.FOURSCORE;
        if (team.getExternalAliases() == null) {
            return false;
        }
        for (TeamExternalAlias alias : team.getExternalAliases()) {
            if (resolvedProvider.equals(alias.getProvider())
                    && externalTeamName.equals(alias.getExternalName())) {
                return true;
            }
        }
        return false;
    }
}
