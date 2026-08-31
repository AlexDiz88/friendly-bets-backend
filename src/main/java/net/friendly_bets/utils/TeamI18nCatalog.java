package net.friendly_bets.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.models.TeamDisplayNames;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Team display names from frontend {@code i18n/*/teams.json} (bundled at build time).
 * Same keys as {@code Team.title}; used when Mongo {@code display_names} are empty.
 */
@Slf4j
@Component
public class TeamI18nCatalog {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final Map<String, String> en;
    private final Map<String, String> ru;
    private final Map<String, String> de;

    public TeamI18nCatalog(ObjectMapper objectMapper) {
        this.en = load(objectMapper, "team-i18n/en/teams.json");
        this.ru = load(objectMapper, "team-i18n/ru/teams.json");
        this.de = load(objectMapper, "team-i18n/de/teams.json");
    }

    public TeamDisplayNames resolveByTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String key = title.trim();
        String enName = en.get(key);
        String ruName = ru.get(key);
        String deName = de.get(key);
        if (enName == null && ruName == null && deName == null) {
            return null;
        }
        return TeamDisplayNames.builder()
                .en(enName)
                .ru(ruName)
                .de(deName)
                .build();
    }

    /** DB display names override bundled i18n when non-blank. */
    public static TeamDisplayNames effectiveDisplayNames(TeamDisplayNames db, TeamDisplayNames i18n) {
        if (db == null && i18n == null) {
            return null;
        }
        return TeamDisplayNames.builder()
                .en(firstNonBlank(db != null ? db.getEn() : null, i18n != null ? i18n.getEn() : null))
                .ru(firstNonBlank(db != null ? db.getRu() : null, i18n != null ? i18n.getRu() : null))
                .de(firstNonBlank(db != null ? db.getDe() : null, i18n != null ? i18n.getDe() : null))
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

    private static Map<String, String> load(ObjectMapper objectMapper, String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            Map<String, String> map = objectMapper.readValue(in, MAP_TYPE);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to load team i18n catalog {}: {}", classpath, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
