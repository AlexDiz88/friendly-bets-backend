package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamTitleUtils;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TeamAliasResolver {

    private static final List<String> NAME_ALIAS_PROVIDERS = List.of(
            TeamTitleUtils.MARATHONBET_PROVIDER,
            TeamTitleUtils.FOURSCORE_PROVIDER,
            TeamTitleUtils.TWENTYFOUR_SCORE_PROVIDER,
            TeamTitleUtils.SOCCER365_PROVIDER
    );

    private final TeamsRepository teamsRepository;

    /**
     * Maps WC FIFA code (KOR, CZE, …) to internal team via country / title / display names /
     * remaining provider aliases — without odds-api.io.
     */
    public Optional<Team> resolveWc26Code(String wc26Code) {
        if (wc26Code == null || wc26Code.isBlank()) {
            return Optional.empty();
        }
        String code = wc26Code.trim();
        Optional<Team> byCountry = teamsRepository.findByCountryIgnoreCase(code);
        if (byCountry.isPresent()) {
            return byCountry;
        }
        for (String candidate : Wc26TeamCatalog.nameCandidatesForFifaCode(code)) {
            Optional<Team> byName = resolveByKnownTeamName(candidate);
            if (byName.isPresent()) {
                return byName;
            }
        }
        return Optional.empty();
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

    public Optional<Team> resolveSoccer365ByName(String soccer365TeamName) {
        if (soccer365TeamName == null || soccer365TeamName.isBlank()) {
            return Optional.empty();
        }
        return teamsRepository.findByExternalAliasName(
                TeamTitleUtils.SOCCER365_PROVIDER, soccer365TeamName.trim());
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

    private Optional<Team> resolveByKnownTeamName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        Optional<Team> byTitle = teamsRepository.findByTitleIgnoreCase(trimmed);
        if (byTitle.isPresent()) {
            return byTitle;
        }
        String compactTitle = trimmed.replaceAll("[^A-Za-z0-9]", "");
        if (!compactTitle.isBlank() && !compactTitle.equalsIgnoreCase(trimmed)) {
            Optional<Team> byCompactTitle = teamsRepository.findByTitleIgnoreCase(compactTitle);
            if (byCompactTitle.isPresent()) {
                return byCompactTitle;
            }
        }
        for (String provider : NAME_ALIAS_PROVIDERS) {
            Optional<Team> byAlias = teamsRepository.findByExternalAliasName(provider, trimmed);
            if (byAlias.isPresent()) {
                return byAlias;
            }
        }
        return findByDisplayName(trimmed);
    }

    private Optional<Team> findByDisplayName(String name) {
        String compact = Wc26TeamCatalog.normalizeCompact(name);
        if (compact.isEmpty()) {
            return Optional.empty();
        }
        for (Team team : teamsRepository.findAll()) {
            TeamDisplayNames names = team.getDisplayNames();
            if (names == null) {
                continue;
            }
            if (compact.equals(Wc26TeamCatalog.normalizeCompact(names.getEn()))
                    || compact.equals(Wc26TeamCatalog.normalizeCompact(names.getRu()))
                    || compact.equals(Wc26TeamCatalog.normalizeCompact(names.getDe()))) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }
}
