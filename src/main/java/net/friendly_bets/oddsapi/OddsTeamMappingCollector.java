package net.friendly_bets.oddsapi;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Счётчик и дедупликация несоответствий odds-api команд за один прогон синхронизации.
 */
public class OddsTeamMappingCollector {

    private final Set<String> recordedSideKeys = new LinkedHashSet<>();
    private int issueCount;

    public int getIssueCount() {
        return issueCount;
    }

    boolean registerSideIssue(String sideKey) {
        if (!recordedSideKeys.add(sideKey)) {
            return false;
        }
        issueCount++;
        return true;
    }
}
