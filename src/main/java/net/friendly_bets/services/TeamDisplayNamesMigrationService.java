package net.friendly_bets.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.dto.TeamDisplayNamesMigrationResultDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.repositories.TeamsRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One-shot admin migration: copy legacy bundled team i18n (ex-frontend teams.json)
 * into {@code teams.display_names} for empty language fields only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamDisplayNamesMigrationService {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final TeamsRepository teamsRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TeamDisplayNamesMigrationResultDto migrate() {
        Map<String, String> en = loadCatalog("en");
        Map<String, String> ru = loadCatalog("ru");
        Map<String, String> de = loadCatalog("de");

        int scanned = 0;
        int updated = 0;
        int alreadyComplete = 0;
        int noCatalogEntry = 0;
        List<String> missingCatalogTitles = new ArrayList<>();

        for (Team team : teamsRepository.findAll()) {
            scanned++;
            String title = team.getTitle();
            if (title == null || title.isBlank()) {
                noCatalogEntry++;
                continue;
            }
            String key = title.trim();
            String enLegacy = en.get(key);
            String ruLegacy = ru.get(key);
            String deLegacy = de.get(key);
            if (enLegacy == null && ruLegacy == null && deLegacy == null) {
                noCatalogEntry++;
                missingCatalogTitles.add(key);
                continue;
            }

            TeamDisplayNames merged = mergeDisplayNames(team.getDisplayNames(), enLegacy, ruLegacy, deLegacy);
            if (displayNamesEqual(team.getDisplayNames(), merged)) {
                alreadyComplete++;
                continue;
            }
            team.setDisplayNames(merged);
            teamsRepository.save(team);
            updated++;
        }

        return TeamDisplayNamesMigrationResultDto.builder()
                .scanned(scanned)
                .updated(updated)
                .alreadyComplete(alreadyComplete)
                .noCatalogEntry(noCatalogEntry)
                .missingCatalogTitles(missingCatalogTitles)
                .build();
    }

    private static TeamDisplayNames mergeDisplayNames(
            TeamDisplayNames current,
            String enLegacy,
            String ruLegacy,
            String deLegacy
    ) {
        return TeamDisplayNames.builder()
                .en(firstNonBlank(current != null ? current.getEn() : null, enLegacy))
                .ru(firstNonBlank(current != null ? current.getRu() : null, ruLegacy))
                .de(firstNonBlank(current != null ? current.getDe() : null, deLegacy))
                .build();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private static boolean displayNamesEqual(TeamDisplayNames a, TeamDisplayNames b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(normalizeField(a.getEn()), normalizeField(b.getEn()))
                && Objects.equals(normalizeField(a.getRu()), normalizeField(b.getRu()))
                && Objects.equals(normalizeField(a.getDe()), normalizeField(b.getDe()));
    }

    private static String normalizeField(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, String> loadCatalog(String lang) {
        String classpath = "migration/team-display-names/" + lang + "/teams.json";
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            Map<String, String> map = objectMapper.readValue(in, MAP_TYPE);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to load legacy team display names catalog {}: {}", classpath, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
