package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalTeamNameChipDto;
import net.friendly_bets.dto.ExternalTeamNamesLoadResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamI18nCatalog;
import net.friendly_bets.utils.TeamNameNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Auto-binds external provider team names to league teams when the name matches
 * displayNames (en/ru/de, including bundled i18n by title when DB fields are empty),
 * team title, or another provider's alias (normalized).
 * Lookup is scoped to the selected league only. Ambiguous / unmatched / mismatched
 * names become chips for manual mapping.
 */
@Service
@RequiredArgsConstructor
public class ExternalTeamAliasAutoBindService {

    private final RunningSeasonLookup runningSeasonLookup;
    private final LeaguesRepository leaguesRepository;
    private final TeamsRepository teamsRepository;
    private final TeamI18nCatalog teamI18nCatalog;
    private final ErrorLogService errorLogService;

    @Transactional
    public ExternalTeamNamesLoadResultDto bindAndCollectUnmapped(
            String provider,
            String leagueCodeRaw,
            List<String> externalNames
    ) {
        return bindAndCollectUnmapped(provider, leagueCodeRaw, externalNames, false);
    }

    @Transactional
    public ExternalTeamNamesLoadResultDto bindAndCollectUnmapped(
            String provider,
            String leagueCodeRaw,
            List<String> externalNames,
            boolean forceOverwrite
    ) {
        if (provider == null || provider.isBlank()) {
            throw new BadRequestException("externalTeamNamesProviderRequired");
        }
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = findLeagueOrThrow(season, leagueCode);
        List<Team> leagueTeams = loadFreshLeagueTeams(league);

        Set<String> uniqueNames = new LinkedHashSet<>();
        if (externalNames != null) {
            for (String raw : externalNames) {
                if (raw != null && !raw.isBlank()) {
                    uniqueNames.add(raw.trim());
                }
            }
        }

        int autoBound = 0;
        int mismatch = 0;
        int overwritten = 0;
        int alreadyMapped = 0;
        List<ExternalTeamNameChipDto> unmapped = new ArrayList<>();
        List<ErrorLogService.TeamAliasMismatchDetail> mismatches = new ArrayList<>();

        Map<String, Team> teamsById = new LinkedHashMap<>();
        for (Team team : leagueTeams) {
            if (team.getId() != null) {
                teamsById.put(team.getId(), team);
            }
        }

        for (String externalName : uniqueNames) {
            Team already = findTeamWithProviderAliasNormalized(teamsById.values(), provider, externalName);
            if (already != null) {
                alreadyMapped++;
                continue;
            }

            List<Team> candidates = findMatchCandidates(teamsById.values(), provider, externalName);
            if (candidates.size() != 1) {
                unmapped.add(chip(provider, externalName));
                continue;
            }

            Team candidate = candidates.get(0);
            TeamExternalAlias existing = findProviderAlias(candidate, provider);
            if (existing != null && existing.getExternalName() != null && !existing.getExternalName().isBlank()) {
                if (TeamNameNormalizer.equalsNormalized(existing.getExternalName(), externalName)) {
                    alreadyMapped++;
                    continue;
                }
                mismatches.add(ErrorLogService.TeamAliasMismatchDetail.builder()
                        .teamId(candidate.getId())
                        .teamTitle(candidate.getTitle())
                        .currentAlias(existing.getExternalName())
                        .incomingAlias(externalName)
                        .build());
                mismatch++;
                if (forceOverwrite) {
                    bindAlias(candidate, provider, externalName);
                    teamsRepository.save(candidate);
                    teamsById.put(candidate.getId(), candidate);
                    errorLogService.purgeTeamMappingIssuesForExternalTeam(provider, externalName);
                    overwritten++;
                } else {
                    alreadyMapped++;
                }
                continue;
            }

            if (hasGlobalProviderExactName(provider, externalName, candidate.getId())) {
                unmapped.add(chip(provider, externalName));
                continue;
            }

            bindAlias(candidate, provider, externalName);
            teamsRepository.save(candidate);
            teamsById.put(candidate.getId(), candidate);
            errorLogService.purgeTeamMappingIssuesForExternalTeam(provider, externalName);
            autoBound++;
        }

        if (!mismatches.isEmpty()) {
            errorLogService.recordTeamAliasMismatchSummary(
                    provider,
                    leagueCode.name(),
                    mismatches,
                    forceOverwrite
            );
        }

        return ExternalTeamNamesLoadResultDto.builder()
                .unmapped(unmapped)
                .autoBoundCount(autoBound)
                .mismatchCount(mismatch)
                .overwrittenCount(overwritten)
                .alreadyMappedCount(alreadyMapped)
                .totalNames(uniqueNames.size())
                .build();
    }

    private void bindAlias(Team team, String provider, String externalName) {
        List<TeamExternalAlias> aliases = team.getExternalAliases();
        if (aliases == null) {
            aliases = new ArrayList<>();
            team.setExternalAliases(aliases);
        }
        aliases.removeIf(a -> a != null && provider.equals(a.getProvider()));
        aliases.add(TeamExternalAlias.builder()
                .provider(provider)
                .externalName(externalName)
                .build());
    }

    private static ExternalTeamNameChipDto chip(String provider, String externalName) {
        return ExternalTeamNameChipDto.builder()
                .externalName(externalName)
                .provider(provider)
                .alreadyMapped(false)
                .build();
    }

    private boolean hasGlobalProviderExactName(String provider, String externalName, String excludeTeamId) {
        return teamsRepository.findByExternalAliasName(provider, externalName)
                .filter(t -> t.getId() != null && !t.getId().equals(excludeTeamId))
                .isPresent();
    }

    private static Team findTeamWithProviderAliasNormalized(
            Iterable<Team> teams,
            String provider,
            String externalName
    ) {
        for (Team team : teams) {
            TeamExternalAlias alias = findProviderAlias(team, provider);
            if (alias != null && TeamNameNormalizer.equalsNormalized(alias.getExternalName(), externalName)) {
                return team;
            }
        }
        return null;
    }

    private List<Team> findMatchCandidates(Iterable<Team> teams, String provider, String externalName) {
        String needle = TeamNameNormalizer.normalize(externalName);
        if (needle.isEmpty()) {
            return List.of();
        }
        Map<String, Team> unique = new HashMap<>();
        for (Team team : teams) {
            if (teamMatchesName(team, provider, needle)) {
                unique.put(team.getId(), team);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private boolean teamMatchesName(Team team, String provider, String normalizedNeedle) {
        if (normalizedEquals(team.getTitle(), normalizedNeedle)) {
            return true;
        }
        TeamDisplayNames names = effectiveDisplayNames(team);
        if (names != null) {
            if (normalizedEquals(names.getEn(), normalizedNeedle)
                    || normalizedEquals(names.getRu(), normalizedNeedle)
                    || normalizedEquals(names.getDe(), normalizedNeedle)) {
                return true;
            }
        }
        if (team.getExternalAliases() == null) {
            return false;
        }
        for (TeamExternalAlias alias : team.getExternalAliases()) {
            if (alias == null || alias.getProvider() == null) {
                continue;
            }
            if (provider.equals(alias.getProvider())) {
                continue;
            }
            if (normalizedEquals(alias.getExternalName(), normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private TeamDisplayNames effectiveDisplayNames(Team team) {
        return TeamI18nCatalog.effectiveDisplayNames(
                team.getDisplayNames(),
                teamI18nCatalog.resolveByTitle(team.getTitle())
        );
    }

    private static boolean normalizedEquals(String value, String normalizedNeedle) {
        return value != null && TeamNameNormalizer.normalize(value).equals(normalizedNeedle);
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

    private List<Team> loadFreshLeagueTeams(League league) {
        List<Team> result = new ArrayList<>();
        League source = league;
        if (league.getId() != null) {
            source = leaguesRepository.findById(league.getId()).orElse(league);
        }
        if (source.getTeams() == null) {
            return result;
        }
        for (Team ref : source.getTeams()) {
            if (ref == null || ref.getId() == null) {
                continue;
            }
            teamsRepository.findById(ref.getId()).ifPresent(result::add);
        }
        return result;
    }

    private static League findLeagueOrThrow(Season season, League.LeagueCode leagueCode) {
        if (season.getLeagues() == null) {
            throw new BadRequestException("leagueNotFoundInSeason");
        }
        return season.getLeagues().stream()
                .filter(Objects::nonNull)
                .filter(l -> leagueCode.equals(l.getLeagueCode()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
    }

    private static League.LeagueCode parseLeagueCode(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }
}
