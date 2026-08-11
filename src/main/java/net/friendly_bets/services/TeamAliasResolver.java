package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamNameNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves teams only via saved {@code external_aliases} for a concrete provider + external_name.
 */
@Component
@RequiredArgsConstructor
public class TeamAliasResolver {

    private final TeamsRepository teamsRepository;

    public Optional<Team> resolveByProviderName(String provider, String externalName) {
        if (provider == null || provider.isBlank() || externalName == null || externalName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = externalName.trim();
        Optional<Team> exact = teamsRepository.findByExternalAliasName(provider, trimmed);
        if (exact.isPresent()) {
            return exact;
        }
        List<Team> linked = teamsRepository.findByExternalAliasProvider(provider);
        for (Team team : linked) {
            if (teamMatchesProviderSide(team, provider, trimmed)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    /** Сопоставление стороны матча с внутренней командой только по alias того же провайдера. */
    public boolean teamMatchesProviderSide(Team team, String provider, String externalTeamName) {
        if (team == null || provider == null || provider.isBlank()
                || externalTeamName == null || externalTeamName.isBlank()) {
            return false;
        }
        TeamExternalAlias providerAlias = findProviderAlias(team, provider);
        if (providerAlias == null) {
            return false;
        }
        String aliasName = providerAlias.getExternalName();
        if (aliasName != null && !aliasName.isBlank()) {
            if (externalTeamName.equals(aliasName)) {
                return true;
            }
            if (TeamNameNormalizer.equalsNormalized(aliasName, externalTeamName)) {
                return true;
            }
        }
        return externalNameMatchesTeamDisplay(team, externalTeamName);
    }

    static boolean externalNameMatchesTeamDisplay(Team team, String externalName) {
        if (team == null || externalName == null || externalName.isBlank()) {
            return false;
        }
        TeamDisplayNames names = team.getDisplayNames();
        if (names != null) {
            if (TeamNameNormalizer.equalsNormalized(names.getEn(), externalName)
                    || TeamNameNormalizer.equalsNormalized(names.getRu(), externalName)
                    || TeamNameNormalizer.equalsNormalized(names.getDe(), externalName)) {
                return true;
            }
        }
        return team.getTitle() != null && TeamNameNormalizer.equalsNormalized(team.getTitle(), externalName);
    }

    private static TeamExternalAlias findProviderAlias(Team team, String provider) {
        if (team.getExternalAliases() == null) {
            return null;
        }
        for (TeamExternalAlias alias : team.getExternalAliases()) {
            if (alias != null && provider.equals(alias.getProvider())) {
                return alias;
            }
        }
        return null;
    }
}
