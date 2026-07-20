package net.friendly_bets.tournamentarchive;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * FIFA 3-letter code (MEX, RSA, …) → Mongo {@link Team#getId()}.
 * Используется при import review-JSON; сами FIFA-коды в коллекцию не пишутся.
 */
@Component
@RequiredArgsConstructor
public class TournamentArchiveTeamResolver {

    /**
     * В БД у части сборных {@code country} не совпадает с FIFA TLA
     * (исторические коды SPA/MOR/SWI/CUR).
     */
    private static final Map<String, List<String>> COUNTRY_ALIASES = Map.of(
            "ESP", List.of("ESP", "SPA"),
            "MAR", List.of("MAR", "MOR"),
            "SUI", List.of("SUI", "SWI"),
            "CUW", List.of("CUW", "CUR")
    );

    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;

    private final Map<String, Team> cache = new HashMap<>();
    private final Set<String> unresolved = new LinkedHashSet<>();

    public void reset() {
        cache.clear();
        unresolved.clear();
    }

    public Set<String> unresolvedCodes() {
        return Set.copyOf(unresolved);
    }

    public String resolveTeamId(String fifaCode) {
        Team team = resolveTeam(fifaCode);
        return team != null ? team.getId() : null;
    }

    public Team resolveTeam(String fifaCode) {
        if (fifaCode == null || fifaCode.isBlank()) {
            return null;
        }
        String code = fifaCode.trim().toUpperCase(Locale.ROOT);
        if (cache.containsKey(code)) {
            return cache.get(code);
        }
        Optional<Team> resolved = teamAliasResolver.resolveWc26Code(code);
        if (resolved.isEmpty()) {
            for (String country : countryCandidates(code)) {
                resolved = teamsRepository.findByCountryIgnoreCase(country);
                if (resolved.isPresent()) {
                    break;
                }
            }
        }
        if (resolved.isPresent()) {
            cache.put(code, resolved.get());
            unresolved.remove(code);
            return resolved.get();
        }
        unresolved.add(code);
        cache.put(code, null);
        return null;
    }

    private static List<String> countryCandidates(String fifaCode) {
        return COUNTRY_ALIASES.getOrDefault(fifaCode, List.of(fifaCode));
    }
}
