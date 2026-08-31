package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamDisplayNamesMigrationResultDto {

    private int scanned;
    private int updated;
    private int alreadyComplete;
    private int noCatalogEntry;
    /** Teams with no legacy i18n entry for title (title keys only). */
    private List<String> missingCatalogTitles;
}
