package net.friendly_bets.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalDataSandboxFullMatchRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxLiveRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxOddsRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxResultDto;
import net.friendly_bets.dto.ExternalDataSandboxScheduleRequestDto;
import net.friendly_bets.dto.MarathonbetMarketDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.marathonbet.MarathonbetExtractedMarkets;
import net.friendly_bets.marathonbet.MarathonbetMarketExtractor;
import net.friendly_bets.marathonbet.MarathonbetPrematchEvent;
import net.friendly_bets.marathonbet.MarathonbetScrapeService;
import net.friendly_bets.marathonbet.MarathonbetTournamentParser;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.soccer365.Soccer365GameParser;
import net.friendly_bets.soccer365.Soccer365HttpClient;
import net.friendly_bets.soccer365.Soccer365ParsedFullMatch;
import net.friendly_bets.soccer365.Soccer365ParsedSchedule;
import net.friendly_bets.soccer365.Soccer365ScheduleParser;
import net.friendly_bets.sportsru.SportsRuCalendarPathSupport;
import net.friendly_bets.sportsru.SportsRuHttpClient;
import net.friendly_bets.sportsru.SportsRuParsedSchedule;
import net.friendly_bets.sportsru.SportsRuScheduleParser;
import net.friendly_bets.twentyfourscore.TwentyFourScoreDatePageParser;
import net.friendly_bets.twentyfourscore.TwentyFourScoreHttpClient;
import net.friendly_bets.twentyfourscore.TwentyFourScoreParsedDatePage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalDataSandboxService {

    private final Soccer365HttpClient soccer365HttpClient;
    private final Soccer365ScheduleParser soccer365ScheduleParser;
    private final Soccer365GameParser soccer365GameParser;
    private final SportsRuHttpClient sportsRuHttpClient;
    private final SportsRuScheduleParser sportsRuScheduleParser;
    private final MarathonbetTournamentClient marathonbetTournamentClient;
    private final MarathonbetScrapeService marathonbetScrapeService;
    private final TwentyFourScoreHttpClient twentyFourScoreHttpClient;
    private final TwentyFourScoreDatePageParser twentyFourScoreDatePageParser;

    public ExternalDataSandboxResultDto runSchedule(ExternalDataSandboxScheduleRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.SOCCER365);
        Integer roundFilter = request != null ? request.getRound() : null;
        if (roundFilter != null && roundFilter <= 0) {
            throw new BadRequestException("sandboxRoundInvalid");
        }
        Integer limit = request != null ? request.getLimit() : null;
        if (limit != null && limit <= 0) {
            throw new BadRequestException("sandboxLimitInvalid");
        }
        long started = System.currentTimeMillis();
        try {
            if (ExternalProviderIds.SOCCER365.equals(provider)) {
                if (request == null || request.getCompetitionId() == null || request.getCompetitionId() <= 0) {
                    throw new BadRequestException("sandboxCompetitionIdRequired");
                }
                int competitionId = request.getCompetitionId();
                String html = soccer365HttpClient.fetchScheduleHtml(competitionId);
                Soccer365ParsedSchedule parsed = soccer365ScheduleParser.parse(html, competitionId);
                return ExternalDataSandboxResultDto.builder()
                        .success(true)
                        .layer(ExternalDataLayer.SCHEDULE.name())
                        .provider(provider)
                        .durationMs(System.currentTimeMillis() - started)
                        .parsed(toSoccer365ScheduleParsed(parsed, roundFilter, limit))
                        .build();
            }
            if (ExternalProviderIds.SPORTS_RU.equals(provider)) {
                String calendarPath = request != null ? request.getCalendarPath() : null;
                String path = SportsRuCalendarPathSupport.resolveCalendarPath(calendarPath);
                if (path == null) {
                    throw new BadRequestException("sandboxCalendarPathRequired");
                }
                String html = sportsRuHttpClient.fetchCalendarHtml(path);
                SportsRuParsedSchedule parsed = sportsRuScheduleParser.parseCalendar(html);
                return ExternalDataSandboxResultDto.builder()
                        .success(true)
                        .layer(ExternalDataLayer.SCHEDULE.name())
                        .provider(provider)
                        .durationMs(System.currentTimeMillis() - started)
                        .parsed(toSportsRuScheduleParsed(parsed, path, roundFilter, limit))
                        .build();
            }
            throw new BadRequestException("sandboxUnsupportedProvider");
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.SCHEDULE, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            String fetchKey = ExternalProviderIds.SPORTS_RU.equals(provider)
                    ? "sportsRuFetchFailed"
                    : "soccer365FetchFailed";
            return fail(ExternalDataLayer.SCHEDULE, provider, started, fetchKey, e.getMessage());
        }
    }

    public ExternalDataSandboxResultDto runOdds(ExternalDataSandboxOddsRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.MARATHONBET);
        if (!ExternalProviderIds.MARATHONBET.equals(provider)) {
            throw new BadRequestException("sandboxUnsupportedProvider");
        }
        if (request == null || request.getTreeId() == null || request.getTreeId() <= 0) {
            throw new BadRequestException("sandboxTreeIdRequired");
        }
        String mode = request.getMode() != null ? request.getMode().trim().toLowerCase(Locale.ROOT) : "";
        if (!"tournament".equals(mode) && !"event".equals(mode)) {
            throw new BadRequestException("sandboxOddsModeRequired");
        }
        long treeId = request.getTreeId();
        long started = System.currentTimeMillis();
        try {
            if ("tournament".equals(mode)) {
                MarathonbetHttpFetchResult fetch = marathonbetTournamentClient.fetchTournament(treeId);
                if (!fetch.isSuccess()) {
                    return fail(ExternalDataLayer.ODDS, provider, started, fetch.toErrorKey(), fetch.getErrorDetail());
                }
                JsonNode body = fetch.getBody();
                List<MarathonbetPrematchEvent> events = MarathonbetTournamentParser.parsePrematchEvents(body);
                return ExternalDataSandboxResultDto.builder()
                        .success(true)
                        .layer(ExternalDataLayer.ODDS.name())
                        .provider(provider)
                        .durationMs(System.currentTimeMillis() - started)
                        .parsed(toOddsTournamentParsed(treeId, events))
                        .build();
            }
            MarathonbetHttpFetchResult fetch = marathonbetScrapeService.fetchEventSnapshotResult(treeId);
            if (!fetch.isSuccess()) {
                return fail(ExternalDataLayer.ODDS, provider, started, fetch.toErrorKey(), fetch.getErrorDetail());
            }
            JsonNode body = fetch.getBody();
            MarathonbetExtractedMarkets markets = MarathonbetMarketExtractor.extractAll(body);
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.ODDS.name())
                    .provider(provider)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(toOddsEventParsed(treeId, markets))
                    .build();
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.ODDS, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            return fail(ExternalDataLayer.ODDS, provider, started, "marathonbetFetchFailed", e.getMessage());
        }
    }

    public ExternalDataSandboxResultDto runLive(ExternalDataSandboxLiveRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.TWENTYFOUR_SCORE);
        if (!ExternalProviderIds.TWENTYFOUR_SCORE.equals(provider)) {
            throw new BadRequestException("sandboxUnsupportedProvider");
        }
        if (request == null || request.getDate() == null || request.getDate().isBlank()) {
            throw new BadRequestException("sandboxDateRequired");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.getDate().trim());
        } catch (RuntimeException e) {
            throw new BadRequestException("sandboxDateRequired");
        }
        String titleContains = request.getTitleContains() != null ? request.getTitleContains().trim() : "";
        long started = System.currentTimeMillis();
        try {
            String html = twentyFourScoreHttpClient.fetchDateFootballHtml(date);
            TwentyFourScoreParsedDatePage page = twentyFourScoreDatePageParser.parse(html);
            List<TwentyFourScoreParsedDatePage.CompetitionBlock> all = page.getCompetitions() != null
                    ? page.getCompetitions()
                    : List.of();
            List<TwentyFourScoreParsedDatePage.CompetitionBlock> filtered = titleContains.isEmpty()
                    ? all
                    : all.stream()
                    .filter(c -> c.getTitle() != null
                            && c.getTitle().toLowerCase(Locale.ROOT).contains(titleContains.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.LIVE.name())
                    .provider(provider)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(toLiveParsed(date.toString(), titleContains, all.size(), filtered))
                    .build();
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.LIVE, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            return fail(ExternalDataLayer.LIVE, provider, started, "twentyFourScoreFetchFailed", e.getMessage());
        }
    }

    public ExternalDataSandboxResultDto runFullMatch(ExternalDataSandboxFullMatchRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.SOCCER365);
        if (!ExternalProviderIds.SOCCER365.equals(provider)) {
            throw new BadRequestException("sandboxUnsupportedProvider");
        }
        if (request == null || request.getGameId() == null || request.getGameId().isBlank()) {
            throw new BadRequestException("sandboxGameIdRequired");
        }
        String gameId = request.getGameId().trim();
        long started = System.currentTimeMillis();
        try {
            String html = soccer365HttpClient.fetchGameHtml(gameId);
            Soccer365ParsedFullMatch parsed = soccer365GameParser.parse(html);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("gameId", gameId);
            summary.put("statusText", parsed.getStatusText());
            summary.put("homeTeamName", parsed.getHomeTeamName());
            summary.put("awayTeamName", parsed.getAwayTeamName());
            summary.put("competitionName", parsed.getCompetitionName());
            summary.put("gameScore", parsed.getGameScore());
            summary.put("goalsCount", parsed.getGoals() != null ? parsed.getGoals().size() : 0);
            summary.put("goals", parsed.getGoals());
            summary.put("stats", parsed.getStats());
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.FULL_MATCH.name())
                    .provider(provider)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(summary)
                    .build();
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.FULL_MATCH, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            return fail(ExternalDataLayer.FULL_MATCH, provider, started, "soccer365FetchFailed", e.getMessage());
        }
    }

    private static String requireProvider(String provider, String defaultProvider) {
        if (provider == null || provider.isBlank()) {
            return defaultProvider;
        }
        return provider.trim();
    }

    private static ExternalDataSandboxResultDto fail(
            ExternalDataLayer layer,
            String provider,
            long started,
            String errorKey,
            String errorDetail
    ) {
        return ExternalDataSandboxResultDto.builder()
                .success(false)
                .layer(layer.name())
                .provider(provider)
                .durationMs(System.currentTimeMillis() - started)
                .errorKey(errorKey)
                .errorDetail(errorDetail)
                .build();
    }

    private Map<String, Object> toSportsRuScheduleParsed(
            SportsRuParsedSchedule parsed,
            String calendarPath,
            Integer roundFilter,
            Integer limit
    ) {
        int matchesTotal = 0;
        for (SportsRuParsedSchedule.Round round : parsed.getRounds()) {
            matchesTotal += round.getMatches() != null ? round.getMatches().size() : 0;
        }

        int remaining = limit != null ? limit : Integer.MAX_VALUE;
        int matchesReturned = 0;
        List<Map<String, Object>> rounds = new ArrayList<>();

        for (SportsRuParsedSchedule.Round round : parsed.getRounds()) {
            if (roundFilter != null && round.getNumber() != roundFilter) {
                continue;
            }
            if (remaining <= 0) {
                break;
            }
            List<Map<String, Object>> matches = new ArrayList<>();
            for (SportsRuParsedSchedule.Match match : round.getMatches()) {
                if (remaining <= 0) {
                    break;
                }
                Instant utcKickoff = null;
                if (match.getMatchPath() != null && !match.getMatchPath().isBlank()) {
                    try {
                        String matchHtml = sportsRuHttpClient.fetchMatchHtml(match.getMatchPath());
                        utcKickoff = sportsRuScheduleParser.parseUtcKickoffFromMatchHtml(matchHtml);
                    } catch (RuntimeException e) {
                        // Keep row without kickoff; calendar parse itself succeeded.
                        utcKickoff = null;
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("homeName", match.getHomeName());
                row.put("awayName", match.getAwayName());
                row.put("utcKickoff", utcKickoff != null ? utcKickoff.toString() : null);
                row.put("status", match.getStatus());
                row.put("matchPath", match.getMatchPath());
                matches.add(row);
                remaining--;
                matchesReturned++;
            }
            if (!matches.isEmpty()) {
                Map<String, Object> roundMap = new LinkedHashMap<>();
                roundMap.put("number", round.getNumber());
                roundMap.put("matches", matches);
                rounds.add(roundMap);
            }
        }

        boolean truncated = matchesReturned < matchesTotal;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("calendarPath", calendarPath);
        out.put("roundFilter", roundFilter);
        out.put("limit", limit);
        out.put("matchesTotal", matchesTotal);
        out.put("roundsTotal", parsed.getRounds().size());
        out.put("roundsCount", rounds.size());
        out.put("matchesCount", matchesReturned);
        out.put("parsedTruncated", truncated);
        out.put("rounds", rounds);
        return out;
    }

    private static Map<String, Object> toSoccer365ScheduleParsed(
            Soccer365ParsedSchedule parsed,
            Integer roundFilter,
            Integer limit
    ) {
        int matchesTotal = 0;
        for (Soccer365ParsedSchedule.Round round : parsed.getRounds()) {
            matchesTotal += round.getMatches() != null ? round.getMatches().size() : 0;
        }

        int remaining = limit != null ? limit : Integer.MAX_VALUE;
        int matchesReturned = 0;
        List<Map<String, Object>> rounds = new ArrayList<>();

        for (Soccer365ParsedSchedule.Round round : parsed.getRounds()) {
            if (roundFilter != null && round.getNumber() != roundFilter) {
                continue;
            }
            if (remaining <= 0) {
                break;
            }
            List<Map<String, Object>> matches = new ArrayList<>();
            for (Soccer365ParsedSchedule.Match match : round.getMatches()) {
                if (remaining <= 0) {
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("homeName", match.getHomeName());
                row.put("awayName", match.getAwayName());
                row.put("utcKickoff", match.getUtcKickoff() != null ? match.getUtcKickoff().toString() : null);
                row.put("status", match.getStatus());
                row.put("soccer365GameId", match.getSoccer365GameId());
                matches.add(row);
                remaining--;
                matchesReturned++;
            }
            if (!matches.isEmpty()) {
                Map<String, Object> roundMap = new LinkedHashMap<>();
                roundMap.put("number", round.getNumber());
                roundMap.put("matches", matches);
                rounds.add(roundMap);
            }
        }

        boolean truncated = matchesReturned < matchesTotal;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("competitionId", parsed.getCompetitionId());
        out.put("roundFilter", roundFilter);
        out.put("limit", limit);
        out.put("matchesTotal", matchesTotal);
        out.put("roundsTotal", parsed.getRounds().size());
        out.put("roundsCount", rounds.size());
        out.put("matchesCount", matchesReturned);
        out.put("parsedTruncated", truncated);
        out.put("rounds", rounds);
        return out;
    }

    private static Map<String, Object> toOddsTournamentParsed(long treeId, List<MarathonbetPrematchEvent> events) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MarathonbetPrematchEvent event : events) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("treeId", event.getTreeId());
            row.put("eventId", event.getEventId());
            row.put("name", event.getName());
            row.put("homeTeam", event.getHomeTeam());
            row.put("awayTeam", event.getAwayTeam());
            row.put("displayTimeMillis", event.getDisplayTimeMillis());
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "tournament");
        out.put("tournamentTreeId", treeId);
        out.put("eventsCount", rows.size());
        out.put("events", rows);
        return out;
    }

    private static Map<String, Object> toOddsEventParsed(long treeId, MarathonbetExtractedMarkets markets) {
        Map<String, Integer> bucketCounts = new LinkedHashMap<>();
        bucketCounts.put("matchResult", size(markets.getMatchResultMarkets()));
        bucketCounts.put("halfTimeResult", size(markets.getHalfTimeResultMarkets()));
        bucketCounts.put("secondHalfResult", size(markets.getSecondHalfResultMarkets()));
        bucketCounts.put("handicap", size(markets.getHandicapMarkets()));
        bucketCounts.put("halfTimeHandicap", size(markets.getHalfTimeHandicapMarkets()));
        bucketCounts.put("secondHalfHandicap", size(markets.getSecondHalfHandicapMarkets()));
        bucketCounts.put("total", size(markets.getTotalMarkets()));
        bucketCounts.put("halfTimeTotal", size(markets.getHalfTimeTotalMarkets()));
        bucketCounts.put("secondHalfTotal", size(markets.getSecondHalfTotalMarkets()));
        bucketCounts.put("teamTotalHome", size(markets.getTeamTotalHomeMarkets()));
        bucketCounts.put("teamTotalAway", size(markets.getTeamTotalAwayMarkets()));
        bucketCounts.put("correctScore", size(markets.getCorrectScoreMarkets()));
        bucketCounts.put("doubleChance", size(markets.getDoubleChanceMarkets()));
        bucketCounts.put("resultTotal", size(markets.getResultTotalMarkets()));
        bucketCounts.put("goals", size(markets.getGoalsMarkets()));
        bucketCounts.put("bttsResult", size(markets.getBttsResultMarkets()));
        bucketCounts.put("playoff", size(markets.getPlayoffMarkets()));

        int totalMarkets = bucketCounts.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "event");
        out.put("eventTreeId", treeId);
        out.put("marketsTotal", totalMarkets);
        out.put("marketBucketCounts", bucketCounts);
        out.put("matchResultMarkets", markets.getMatchResultMarkets());
        out.put("handicapMarkets", markets.getHandicapMarkets());
        out.put("totalMarkets", markets.getTotalMarkets());
        out.put("doubleChanceMarkets", markets.getDoubleChanceMarkets());
        return out;
    }

    private static int size(List<MarathonbetMarketDto> list) {
        return list != null ? list.size() : 0;
    }

    private static Map<String, Object> toLiveParsed(
            String date,
            String titleContains,
            int competitionsTotal,
            List<TwentyFourScoreParsedDatePage.CompetitionBlock> filtered
    ) {
        int matchesCount = 0;
        List<Map<String, Object>> competitions = new ArrayList<>();
        for (TwentyFourScoreParsedDatePage.CompetitionBlock block : filtered) {
            List<Map<String, Object>> matches = new ArrayList<>();
            for (TwentyFourScoreParsedDatePage.MatchRow match : block.getMatches()) {
                matchesCount++;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("externalMatchId", match.getExternalMatchId());
                row.put("homeName", match.getHomeName());
                row.put("awayName", match.getAwayName());
                row.put("scoreText", match.getScoreText());
                row.put("fullTimeScore", match.getFullTimeScore());
                row.put("firstTimeScore", match.getFirstTimeScore());
                row.put("liveMinuteLabel", match.getLiveMinuteLabel());
                row.put("status", match.getStatus());
                matches.add(row);
            }
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("title", block.getTitle());
            comp.put("matches", matches);
            competitions.add(comp);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date);
        out.put("titleContains", titleContains.isEmpty() ? null : titleContains);
        out.put("competitionsTotal", competitionsTotal);
        out.put("competitionsMatched", competitions.size());
        out.put("matchesCount", matchesCount);
        out.put("competitions", competitions);
        return out;
    }
}
