package net.friendly_bets.api_football;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.api_football.client.ApiFootballClient;
import net.friendly_bets.api_football.client.dto.ApiFootballFixtureDto;
import net.friendly_bets.api_football.config.ApiFootballProperties;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.services.TeamAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiFootballSecondarySyncService {

    private static final Logger log = LoggerFactory.getLogger(ApiFootballSecondarySyncService.class);

    private static final Map<String, Integer> LEAGUE_CODE_TO_API_FOOTBALL_ID = Map.of(
            "EPL", 39,
            "ELC", 40,
            "BL1", 78,
            "SA", 135,
            "PD", 140,
            "FL1", 61,
            "CL", 2,
            "EL", 3,
            "EC", 4,
            "WC", 1
    );

    private final ApiFootballProperties properties;
    private final ApiFootballClient client;
    private final ApiFootballScoreNormalizer scoreNormalizer;
    private final TeamAliasResolver teamAliasResolver;
    private final MatchResultSyncSettingsService settingsService;

    public void enrichIncoming(
            GameResultRecord incoming,
            Team homeTeam,
            Team awayTeam,
            String leagueCode,
            String seasonYear,
            LocalDateTime fetchedAt
    ) {
        if (!shouldFetchSecondary()) {
            return;
        }
        Integer apiLeagueId = LEAGUE_CODE_TO_API_FOOTBALL_ID.get(leagueCode);
        if (apiLeagueId == null || incoming.getUtcDate() == null) {
            return;
        }
        LocalDate matchDate = incoming.getUtcDate().toLocalDate();
        int season = parseSeasonYear(seasonYear);
        try {
            List<ApiFootballFixtureDto> fixtures = client.fetchFixturesByDate(apiLeagueId, season, matchDate);
            Optional<ApiFootballFixtureDto> match = findMatchingFixture(fixtures, homeTeam, awayTeam);
            if (match.isEmpty()) {
                log.debug("api-football: no fixture for {} vs {} on {}", homeTeam.getTitle(), awayTeam.getTitle(), matchDate);
                return;
            }
            attachSource(incoming, match.get(), fetchedAt);
        } catch (Exception e) {
            log.warn("api-football enrich failed for {}: {}", leagueCode, e.getMessage());
        }
    }

    private boolean shouldFetchSecondary() {
        if (!properties.isSyncEnabled() || !client.isConfigured()) {
            return false;
        }
        return settingsService.getEffective().isDualVerificationEnabled();
    }

    private Optional<ApiFootballFixtureDto> findMatchingFixture(
            List<ApiFootballFixtureDto> fixtures,
            Team homeTeam,
            Team awayTeam
    ) {
        if (fixtures == null || fixtures.isEmpty()) {
            return Optional.empty();
        }
        for (ApiFootballFixtureDto fixture : fixtures) {
            if (fixture.getTeams() == null || fixture.getTeams().getHome() == null || fixture.getTeams().getAway() == null) {
                continue;
            }
            int homeId = fixture.getTeams().getHome().getId();
            int awayId = fixture.getTeams().getAway().getId();
            if (teamAliasResolver.teamMatchesApiFootballSide(homeTeam, homeId, fixture.getTeams().getHome().getName())
                    && teamAliasResolver.teamMatchesApiFootballSide(awayTeam, awayId, fixture.getTeams().getAway().getName())) {
                return Optional.of(fixture);
            }
        }
        return Optional.empty();
    }

    private void attachSource(GameResultRecord incoming, ApiFootballFixtureDto fixture, LocalDateTime fetchedAt) {
        GameResultSourceSnapshot snapshot = GameResultSourceSnapshot.builder()
                .externalMatchId(fixture.getFixture().getId())
                .status(fixture.getFixture().getStatus() != null ? fixture.getFixture().getStatus().getShortStatus() : null)
                .utcDate(incoming.getUtcDate())
                .gameScore(scoreNormalizer.normalize(fixture))
                .scoreDuration(scoreNormalizer.resolveDuration(fixture))
                .home(GameResultSideSnapshot.builder()
                        .externalId(String.valueOf(fixture.getTeams().getHome().getId()))
                        .externalName(fixture.getTeams().getHome().getName())
                        .build())
                .away(GameResultSideSnapshot.builder()
                        .externalId(String.valueOf(fixture.getTeams().getAway().getId()))
                        .externalName(fixture.getTeams().getAway().getName())
                        .build())
                .fetchedAt(fetchedAt)
                .build();

        Map<String, GameResultSourceSnapshot> sources = incoming.getSources();
        if (sources == null) {
            sources = new HashMap<>();
            incoming.setSources(sources);
        }
        sources.put(MatchDataProviders.sourcesStorageKey(MatchDataProviders.API_FOOTBALL), snapshot);
    }

    private static int parseSeasonYear(String seasonYear) {
        try {
            return Integer.parseInt(seasonYear.trim());
        } catch (NumberFormatException e) {
            return LocalDate.now().getYear();
        }
    }
}
