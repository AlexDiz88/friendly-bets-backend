package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MatchdayPollingTargetResolver {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final MatchdaySlotSupport matchdaySlotSupport;
    private final GameResultPollingFilter gameResultPollingFilter;

    /**
     * Туры running-сезона с не финализированными матчами, которые уже начались или идут live.
     * Не зависит от OPENED-ставок.
     */
    public Set<MatchdaySlotKey> collectForSeason(Season season, Collection<String> enabledLeagueCodes) {
        Set<MatchdaySlotKey> targets = new LinkedHashSet<>();
        if (season == null || enabledLeagueCodes == null || enabledLeagueCodes.isEmpty()) {
            return targets;
        }
        for (String leagueCode : enabledLeagueCodes) {
            if (leagueCode == null || leagueCode.isBlank()) {
                continue;
            }
            League.LeagueCode parsedLeague;
            try {
                parsedLeague = League.LeagueCode.valueOf(leagueCode.trim());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!LeagueCompetitionMapping.isSupported(parsedLeague)) {
                continue;
            }
            String externalSeason = matchdaySlotSupport.resolveExternalSeasonYear(season, parsedLeague);
            List<GameResultRecord> records = gameResultRecordRepository.findByLeagueCodeAndSeason(
                    parsedLeague.name(),
                    externalSeason
            );
            for (GameResultRecord record : records) {
                if (!gameResultPollingFilter.needsExternalPoll(record)) {
                    continue;
                }
                String leagueId = record.getLeagueId();
                if (leagueId == null || leagueId.isBlank()) {
                    continue;
                }
                targets.add(new MatchdaySlotKey(
                        record.getLeagueCode(),
                        record.getMatchday(),
                        record.getSeason(),
                        leagueId
                ));
            }
        }
        return targets;
    }
}
