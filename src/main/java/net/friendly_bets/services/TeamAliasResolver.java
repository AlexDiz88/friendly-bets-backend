package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.TeamsRepository;
import org.springframework.stereotype.Component;

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
        return teamsRepository.findByExternalAliasName(provider, externalName.trim());
    }

    /** Сопоставление стороны матча с внутренней командой только по alias того же провайдера. */
    public boolean teamMatchesProviderSide(Team team, String provider, String externalTeamName) {
        if (team == null || provider == null || provider.isBlank()
                || externalTeamName == null || externalTeamName.isBlank()) {
            return false;
        }
        if (team.getExternalAliases() == null) {
            return false;
        }
        for (TeamExternalAlias alias : team.getExternalAliases()) {
            if (provider.equals(alias.getProvider())
                    && externalTeamName.equals(alias.getExternalName())) {
                return true;
            }
        }
        return false;
    }
}
