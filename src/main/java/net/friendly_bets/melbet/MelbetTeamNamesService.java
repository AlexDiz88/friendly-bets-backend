package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.melbet.client.MelbetHttpClient;
import net.friendly_bets.melbet.client.MelbetHttpFetchResult;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Collects Melbet team display names for admin aliases.
 * Uses a light tournament list ({@code stakeTypes=0}) — events + HT/AT only, no odds markets.
 */
@Service
@RequiredArgsConstructor
public class MelbetTeamNamesService {

    private final MelbetProperties properties;
    private final MelbetHttpClient httpClient;
    private final RunningSeasonLookup runningSeasonLookup;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");

        Long tournamentId = properties.tournamentIdForLeague(leagueCode.name());
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("melbetInvalidTournamentId");
        }

        MelbetHttpFetchResult listResult = httpClient.fetchTournamentEventsForTeamNames(tournamentId);
        if (!listResult.isSuccess() || listResult.getBody() == null) {
            throw new BadRequestException(
                    listResult.getOutcome() != null ? listResult.toErrorKey() : "melbetFetchFailed");
        }

        List<MelbetPrematchEvent> events = MelbetTournamentParser.parsePrematchEvents(listResult.getBody());
        Set<String> uniqueNames = new LinkedHashSet<>();
        Set<String> normalizedSeen = new LinkedHashSet<>();
        for (MelbetPrematchEvent event : events) {
            // One name per side. Prefer RU (HT/AT) — same language as most other providers' aliases.
            addName(uniqueNames, normalizedSeen, firstNonBlank(event.getHomeTeam(), event.getHomeTeamEn()));
            addName(uniqueNames, normalizedSeen, firstNonBlank(event.getAwayTeam(), event.getAwayTeamEn()));
        }
        if (uniqueNames.isEmpty()) {
            throw new BadRequestException("melbetTeamNamesEmpty");
        }
        return new ArrayList<>(uniqueNames);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static void addName(Set<String> names, Set<String> normalizedSeen, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String trimmed = raw.trim();
        String key = trimmed.toLowerCase(Locale.ROOT);
        if (!normalizedSeen.add(key)) {
            return;
        }
        names.add(trimmed);
    }
}
