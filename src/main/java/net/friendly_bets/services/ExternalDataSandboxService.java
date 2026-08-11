package net.friendly_bets.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.championat.ChampionatDateJsonParser;
import net.friendly_bets.championat.ChampionatHttpClient;
import net.friendly_bets.championat.ChampionatParsedDatePage;
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
import net.friendly_bets.melbet.MelbetAllowedMarkets;
import net.friendly_bets.melbet.MelbetMarketBucket;
import net.friendly_bets.melbet.MelbetPrematchEvent;
import net.friendly_bets.melbet.MelbetTournamentParser;
import net.friendly_bets.melbet.client.MelbetHttpClient;
import net.friendly_bets.melbet.client.MelbetHttpFetchResult;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.live.LiveMatchSnapshot;
import net.friendly_bets.football24.Football24HttpClient;
import net.friendly_bets.football24.Football24ParsedSchedule;
import net.friendly_bets.football24.Football24ScheduleParser;
import net.friendly_bets.soccer365.Soccer365GameParser;
import net.friendly_bets.soccer365.Soccer365HttpClient;
import net.friendly_bets.soccer365.Soccer365ParsedFullMatch;
import net.friendly_bets.soccer365.Soccer365ParsedSchedule;
import net.friendly_bets.soccer365.Soccer365ScheduleParser;
import net.friendly_bets.sportsru.SportsRuCalendarPathSupport;
import net.friendly_bets.sportsru.SportsRuHttpClient;
import net.friendly_bets.sportsru.SportsRuParsedSchedule;
import net.friendly_bets.sportsru.SportsRuScheduleParser;
import net.friendly_bets.ruscore.RuscoreDayPageParser;
import net.friendly_bets.ruscore.RuscoreGameSummaryParser;
import net.friendly_bets.ruscore.RuscoreHttpClient;
import net.friendly_bets.ruscore.RuscoreParsedDayPage;
import net.friendly_bets.ruscore.RuscoreParsedFullMatch;
import net.friendly_bets.flashscore.FlashscoreDayFeedParser;
import net.friendly_bets.flashscore.FlashscoreHttpClient;
import net.friendly_bets.flashscore.FlashscoreMatchDetailParser;
import net.friendly_bets.flashscore.FlashscoreParsedDayPage;
import net.friendly_bets.flashscore.FlashscoreParsedFullMatch;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.twentyfourscore.TwentyFourScoreDatePageParser;
import net.friendly_bets.twentyfourscore.TwentyFourScoreHttpClient;
import net.friendly_bets.twentyfourscore.TwentyFourScoreParsedDatePage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalDataSandboxService {

    private final Soccer365HttpClient soccer365HttpClient;
    private final Soccer365ScheduleParser soccer365ScheduleParser;
    private final Soccer365GameParser soccer365GameParser;
    private final SportsRuHttpClient sportsRuHttpClient;
    private final SportsRuScheduleParser sportsRuScheduleParser;
    private final Football24HttpClient football24HttpClient;
    private final Football24ScheduleParser football24ScheduleParser;
    private final MarathonbetTournamentClient marathonbetTournamentClient;
    private final MarathonbetScrapeService marathonbetScrapeService;
    private final MelbetHttpClient melbetHttpClient;
    private final TwentyFourScoreHttpClient twentyFourScoreHttpClient;
    private final TwentyFourScoreDatePageParser twentyFourScoreDatePageParser;
    private final ChampionatHttpClient championatHttpClient;
    private final ChampionatDateJsonParser championatDateJsonParser;
    private final RuscoreHttpClient ruscoreHttpClient;
    private final RuscoreDayPageParser ruscoreDayPageParser;
    private final RuscoreGameSummaryParser ruscoreGameSummaryParser;
    private final FlashscoreHttpClient flashscoreHttpClient;
    private final FlashscoreDayFeedParser flashscoreDayFeedParser;
    private final FlashscoreMatchDetailParser flashscoreMatchDetailParser;
    private final TeamAliasResolver teamAliasResolver;

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
            if (ExternalProviderIds.FOOTBALL24.equals(provider)) {
                if (request == null || request.getCompetitionId() == null || request.getCompetitionId() <= 0) {
                    throw new BadRequestException("sandboxCompetitionIdRequired");
                }
                int leagueId = request.getCompetitionId();
                String seasonsJson = football24HttpClient.fetchSeasonsJson(leagueId);
                int seasonYear = Year.now(ZoneOffset.UTC).getValue();
                OptionalInt seasonIdOpt = football24ScheduleParser.resolveSeasonId(seasonsJson, seasonYear);
                if (seasonIdOpt.isEmpty()) {
                    throw new BadRequestException("football24SeasonUnresolved");
                }
                int seasonId = seasonIdOpt.getAsInt();
                String fixturesJson = football24HttpClient.fetchFixturesRoundsJson(seasonId);
                Football24ParsedSchedule parsed = football24ScheduleParser.parseFixturesRounds(fixturesJson, seasonId);
                return ExternalDataSandboxResultDto.builder()
                        .success(true)
                        .layer(ExternalDataLayer.SCHEDULE.name())
                        .provider(provider)
                        .durationMs(System.currentTimeMillis() - started)
                        .parsed(toFootball24ScheduleParsed(parsed, leagueId, roundFilter, limit))
                        .build();
            }
            throw new BadRequestException("sandboxUnsupportedProvider");
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.SCHEDULE, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            String fetchKey = ExternalProviderIds.SPORTS_RU.equals(provider)
                    ? "sportsRuFetchFailed"
                    : (ExternalProviderIds.FOOTBALL24.equals(provider)
                    ? "football24FetchFailed"
                    : "soccer365FetchFailed");
            return fail(ExternalDataLayer.SCHEDULE, provider, started, fetchKey, e.getMessage());
        }
    }

    public ExternalDataSandboxResultDto runOdds(ExternalDataSandboxOddsRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.MARATHONBET);
        if (!ExternalProviderIds.MARATHONBET.equals(provider) && !ExternalProviderIds.MELBET.equals(provider)) {
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
            if (ExternalProviderIds.MELBET.equals(provider)) {
                return runMelbetOdds(mode, treeId, started);
            }
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
            String fetchKey = ExternalProviderIds.MELBET.equals(provider) ? "melbetFetchFailed" : "marathonbetFetchFailed";
            return fail(ExternalDataLayer.ODDS, provider, started, fetchKey, e.getMessage());
        }
    }

    private ExternalDataSandboxResultDto runMelbetOdds(String mode, long id, long started) {
        if ("tournament".equals(mode)) {
            MelbetHttpFetchResult fetch = melbetHttpClient.fetchTournamentEvents(id);
            if (!fetch.isSuccess()) {
                return fail(ExternalDataLayer.ODDS, ExternalProviderIds.MELBET, started, fetch.toErrorKey(), fetch.getErrorDetail());
            }
            List<MelbetPrematchEvent> events = MelbetTournamentParser.parsePrematchEvents(fetch.getBody());
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.ODDS.name())
                    .provider(ExternalProviderIds.MELBET)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(toMelbetOddsTournamentParsed(id, events))
                    .build();
        }
        MelbetHttpFetchResult fetch = melbetHttpClient.fetchEvent(id);
        if (!fetch.isSuccess()) {
            return fail(ExternalDataLayer.ODDS, ExternalProviderIds.MELBET, started, fetch.toErrorKey(), fetch.getErrorDetail());
        }
        return ExternalDataSandboxResultDto.builder()
                .success(true)
                .layer(ExternalDataLayer.ODDS.name())
                .provider(ExternalProviderIds.MELBET)
                .durationMs(System.currentTimeMillis() - started)
                .parsed(toMelbetOddsEventParsed(id, fetch.getBody()))
                .build();
    }

    public ExternalDataSandboxResultDto runLive(ExternalDataSandboxLiveRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.TWENTYFOUR_SCORE);
        if (!ExternalProviderIds.TWENTYFOUR_SCORE.equals(provider)
                && !ExternalProviderIds.CHAMPIONAT.equals(provider)) {
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
            if (ExternalProviderIds.CHAMPIONAT.equals(provider)) {
                String json = championatHttpClient.fetchDateFootballJson(date);
                ChampionatParsedDatePage page = championatDateJsonParser.parse(json);
                List<ChampionatParsedDatePage.CompetitionBlock> all = page.getCompetitions() != null
                        ? page.getCompetitions()
                        : List.of();
                List<ChampionatParsedDatePage.CompetitionBlock> filtered = titleContains.isEmpty()
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
                        .parsed(toChampionatLiveParsed(date.toString(), titleContains, all.size(), filtered, provider))
                        .build();
            }
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
                    .parsed(toLiveParsed(date.toString(), titleContains, all.size(), filtered, provider))
                    .build();
        } catch (BadRequestException e) {
            return fail(ExternalDataLayer.LIVE, provider, started, e.getMessage(), null);
        } catch (RuntimeException e) {
            String fetchKey = ExternalProviderIds.CHAMPIONAT.equals(provider)
                    ? "championatFetchFailed"
                    : "twentyFourScoreFetchFailed";
            return fail(ExternalDataLayer.LIVE, provider, started, fetchKey, e.getMessage());
        }
    }

    public ExternalDataSandboxResultDto runFullMatch(ExternalDataSandboxFullMatchRequestDto request) {
        String provider = requireProvider(request != null ? request.getProvider() : null, ExternalProviderIds.SOCCER365);
        if (!ExternalProviderIds.SOCCER365.equals(provider)
                && !ExternalProviderIds.RUSCORE.equals(provider)
                && !ExternalProviderIds.FLASHSCORE.equals(provider)) {
            throw new BadRequestException("sandboxUnsupportedProvider");
        }
        long started = System.currentTimeMillis();
        try {
            if (ExternalProviderIds.RUSCORE.equals(provider)) {
                return runRuscoreFullMatch(request, started);
            }
            if (ExternalProviderIds.FLASHSCORE.equals(provider)) {
                return runFlashscoreFullMatch(request, started);
            }
            if (request == null || request.getGameId() == null || request.getGameId().isBlank()) {
                throw new BadRequestException("sandboxGameIdRequired");
            }
            String gameId = request.getGameId().trim();
            String html = soccer365HttpClient.fetchGameHtml(gameId);
            Soccer365ParsedFullMatch parsed = soccer365GameParser.parse(html);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("gameId", gameId);
            summary.put("statusText", parsed.getStatusText());
            summary.put("homeTeamName", parsed.getHomeTeamName());
            summary.put("awayTeamName", parsed.getAwayTeamName());
            summary.put("homeTeam", resolveSandboxTeam(provider, parsed.getHomeTeamName()));
            summary.put("awayTeam", resolveSandboxTeam(provider, parsed.getAwayTeamName()));
            summary.put("competitionName", parsed.getCompetitionName());
            summary.put("gameScore", parsed.getGameScore());
            summary.put("goalsCount", countNonCardGoals(parsed.getGoals()));
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
            String fetchKey = ExternalProviderIds.RUSCORE.equals(provider)
                    ? "ruscoreFetchFailed"
                    : (ExternalProviderIds.FLASHSCORE.equals(provider)
                    ? "flashscoreFetchFailed"
                    : "soccer365FetchFailed");
            return fail(ExternalDataLayer.FULL_MATCH, provider, started, fetchKey, e.getMessage());
        }
    }

    private ExternalDataSandboxResultDto runRuscoreFullMatch(
            ExternalDataSandboxFullMatchRequestDto request,
            long started
    ) {
        String gameIdRaw = request != null && request.getGameId() != null ? request.getGameId().trim() : "";
        String dateRaw = request != null && request.getDate() != null ? request.getDate().trim() : "";
        if (!gameIdRaw.isEmpty()) {
            String slug;
            String eventId;
            int slash = gameIdRaw.lastIndexOf('/');
            if (slash > 0 && slash < gameIdRaw.length() - 1) {
                slug = gameIdRaw.substring(0, slash).trim();
                eventId = gameIdRaw.substring(slash + 1).trim();
            } else {
                // eventId only — day browse should supply slug; allow numeric id via /game lookup is not supported
                throw new BadRequestException("sandboxGameIdRequired");
            }
            if (slug.isEmpty() || eventId.isEmpty()) {
                throw new BadRequestException("sandboxGameIdRequired");
            }
            // Accept "slug/eventId" or full path remnant
            if (slug.contains("/")) {
                int last = slug.lastIndexOf('/');
                slug = slug.substring(last + 1);
            }
            String html = ruscoreHttpClient.fetchGameSummaryHtml(slug, eventId);
            RuscoreParsedFullMatch parsed = ruscoreGameSummaryParser.parse(html, eventId, slug);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("gameId", slug + "/" + eventId);
            summary.put("eventId", eventId);
            summary.put("slug", slug);
            summary.put("statusText", parsed.getStatusText());
            summary.put("homeTeamName", parsed.getHomeTeamName());
            summary.put("awayTeamName", parsed.getAwayTeamName());
            summary.put("homeTeam", resolveSandboxTeam(ExternalProviderIds.RUSCORE, parsed.getHomeTeamName()));
            summary.put("awayTeam", resolveSandboxTeam(ExternalProviderIds.RUSCORE, parsed.getAwayTeamName()));
            summary.put("competitionName", parsed.getCompetitionName());
            summary.put("gameScore", parsed.getGameScore());
            summary.put("goalsCount", countNonCardGoals(parsed.getGoals()));
            summary.put("goals", parsed.getGoals());
            summary.put("stats", parsed.getStats());
            summary.put("addedTimeFirstHalf", parsed.getAddedTimeFirstHalf());
            summary.put("addedTimeSecondHalf", parsed.getAddedTimeSecondHalf());
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.FULL_MATCH.name())
                    .provider(ExternalProviderIds.RUSCORE)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(summary)
                    .build();
        }
        if (dateRaw.isEmpty()) {
            throw new BadRequestException("sandboxDateRequired");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateRaw);
        } catch (RuntimeException e) {
            throw new BadRequestException("sandboxDateRequired");
        }
        String titleContains = request.getTitleContains() != null ? request.getTitleContains().trim() : "";
        String html = ruscoreHttpClient.fetchDayFootballHtml(date);
        RuscoreParsedDayPage page = ruscoreDayPageParser.parse(html, date);
        List<RuscoreParsedDayPage.CompetitionBlock> all = page.getCompetitions() != null
                ? page.getCompetitions()
                : List.of();
        List<RuscoreParsedDayPage.CompetitionBlock> filtered = titleContains.isEmpty()
                ? all
                : all.stream()
                .filter(c -> RuscoreDayPageParser.competitionMatchesFilter(c, titleContains))
                .collect(Collectors.toList());
        return ExternalDataSandboxResultDto.builder()
                .success(true)
                .layer(ExternalDataLayer.FULL_MATCH.name())
                .provider(ExternalProviderIds.RUSCORE)
                .durationMs(System.currentTimeMillis() - started)
                .parsed(toRuscoreDayParsed(date.toString(), titleContains, all.size(), filtered))
                .build();
    }

    private ExternalDataSandboxResultDto runFlashscoreFullMatch(
            ExternalDataSandboxFullMatchRequestDto request,
            long started
    ) {
        String gameIdRaw = request != null && request.getGameId() != null ? request.getGameId().trim() : "";
        String dateRaw = request != null && request.getDate() != null ? request.getDate().trim() : "";
        if (!gameIdRaw.isEmpty()) {
            String eventId = gameIdRaw;
            int slash = gameIdRaw.lastIndexOf('/');
            if (slash > 0 && slash < gameIdRaw.length() - 1) {
                eventId = gameIdRaw.substring(slash + 1).trim();
            }
            if (eventId.isEmpty()) {
                throw new BadRequestException("sandboxGameIdRequired");
            }
            String summaryFeed = flashscoreHttpClient.fetchMatchSummaryFeed(eventId);
            String statsFeed = flashscoreHttpClient.fetchMatchStatsFeed(eventId);
            String resultFeed = flashscoreHttpClient.fetchMatchResultFeed(eventId);
            String h2hFeed = flashscoreHttpClient.fetchMatchH2HFeed(eventId);
            FlashscoreParsedFullMatch parsed = flashscoreMatchDetailParser.parse(
                    summaryFeed, statsFeed, resultFeed, eventId, h2hFeed);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("gameId", eventId);
            summary.put("eventId", eventId);
            summary.put("statusText", parsed.getStatusText());
            summary.put("homeTeamName", parsed.getHomeTeamName());
            summary.put("awayTeamName", parsed.getAwayTeamName());
            summary.put("homeTeam", resolveSandboxTeam(ExternalProviderIds.FLASHSCORE, parsed.getHomeTeamName()));
            summary.put("awayTeam", resolveSandboxTeam(ExternalProviderIds.FLASHSCORE, parsed.getAwayTeamName()));
            summary.put("competitionName", parsed.getCompetitionName());
            summary.put("gameScore", parsed.getGameScore());
            summary.put("goalsCount", countNonCardGoals(parsed.getGoals()));
            summary.put("goals", parsed.getGoals());
            summary.put("stats", parsed.getStats());
            summary.put("addedTimeFirstHalf", parsed.getAddedTimeFirstHalf());
            summary.put("addedTimeSecondHalf", parsed.getAddedTimeSecondHalf());
            return ExternalDataSandboxResultDto.builder()
                    .success(true)
                    .layer(ExternalDataLayer.FULL_MATCH.name())
                    .provider(ExternalProviderIds.FLASHSCORE)
                    .durationMs(System.currentTimeMillis() - started)
                    .parsed(summary)
                    .build();
        }
        if (dateRaw.isEmpty()) {
            throw new BadRequestException("sandboxDateRequired");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateRaw);
        } catch (RuntimeException e) {
            throw new BadRequestException("sandboxDateRequired");
        }
        String titleContains = request.getTitleContains() != null ? request.getTitleContains().trim() : "";
        String feed = flashscoreHttpClient.fetchDayFootballFeed(date);
        FlashscoreParsedDayPage page = flashscoreDayFeedParser.parse(feed, date);
        List<FlashscoreParsedDayPage.CompetitionBlock> all = page.getCompetitions() != null
                ? page.getCompetitions()
                : List.of();
        List<FlashscoreParsedDayPage.CompetitionBlock> filtered = titleContains.isEmpty()
                ? all
                : all.stream()
                .filter(c -> FlashscoreDayFeedParser.competitionMatchesFilter(c, titleContains))
                .collect(Collectors.toList());
        return ExternalDataSandboxResultDto.builder()
                .success(true)
                .layer(ExternalDataLayer.FULL_MATCH.name())
                .provider(ExternalProviderIds.FLASHSCORE)
                .durationMs(System.currentTimeMillis() - started)
                .parsed(toFlashscoreDayParsed(date.toString(), titleContains, all.size(), filtered))
                .build();
    }

    private Map<String, Object> toFlashscoreDayParsed(
            String date,
            String titleContains,
            int competitionsTotal,
            List<FlashscoreParsedDayPage.CompetitionBlock> filtered
    ) {
        int matchesCount = 0;
        List<Map<String, Object>> competitions = new ArrayList<>();
        for (FlashscoreParsedDayPage.CompetitionBlock block : filtered) {
            List<Map<String, Object>> matches = new ArrayList<>();
            if (block.getMatches() != null) {
                for (FlashscoreParsedDayPage.Match match : block.getMatches()) {
                    matchesCount++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("externalMatchId", match.getEventId());
                    row.put("gameId", match.getEventId());
                    row.put("homeName", match.getHomeName());
                    row.put("awayName", match.getAwayName());
                    row.put("homeTeam", resolveSandboxTeam(ExternalProviderIds.FLASHSCORE, match.getHomeName()));
                    row.put("awayTeam", resolveSandboxTeam(ExternalProviderIds.FLASHSCORE, match.getAwayName()));
                    row.put("utcKickoff", match.getUtcKickoff() != null ? match.getUtcKickoff().toString() : null);
                    row.put("status", match.getStatusText());
                    row.put("scoreText", match.getScoreText());
                    matches.add(row);
                }
            }
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("title", block.getTitle());
            comp.put("stageId", block.getStageId());
            comp.put("tournamentPath", block.getTournamentPath());
            comp.put("matches", matches);
            competitions.add(comp);
        }
        return liveParsedEnvelope(date, titleContains, competitionsTotal, matchesCount, competitions);
    }

    private Map<String, Object> toRuscoreDayParsed(
            String date,
            String titleContains,
            int competitionsTotal,
            List<RuscoreParsedDayPage.CompetitionBlock> filtered
    ) {
        int matchesCount = 0;
        List<Map<String, Object>> competitions = new ArrayList<>();
        for (RuscoreParsedDayPage.CompetitionBlock block : filtered) {
            List<Map<String, Object>> matches = new ArrayList<>();
            if (block.getMatches() != null) {
                for (RuscoreParsedDayPage.Match match : block.getMatches()) {
                    matchesCount++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("externalMatchId", match.getEventId());
                    row.put("gameId", match.getSlug() + "/" + match.getEventId());
                    row.put("slug", match.getSlug());
                    row.put("homeName", match.getHomeName());
                    row.put("awayName", match.getAwayName());
                    row.put("homeTeam", resolveSandboxTeam(ExternalProviderIds.RUSCORE, match.getHomeName()));
                    row.put("awayTeam", resolveSandboxTeam(ExternalProviderIds.RUSCORE, match.getAwayName()));
                    row.put("utcKickoff", match.getUtcKickoff() != null ? match.getUtcKickoff().toString() : null);
                    row.put("status", match.getStatusText());
                    row.put("scoreText", match.getScoreText());
                    matches.add(row);
                }
            }
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("title", block.getTitle());
            comp.put("seasonId", block.getSeasonId());
            comp.put("tournamentSlug", block.getTournamentSlug());
            comp.put("matches", matches);
            competitions.add(comp);
        }
        return liveParsedEnvelope(date, titleContains, competitionsTotal, matchesCount, competitions);
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

    private static Map<String, Object> toFootball24ScheduleParsed(
            Football24ParsedSchedule parsed,
            int leagueId,
            Integer roundFilter,
            Integer limit
    ) {
        int matchesTotal = 0;
        for (Football24ParsedSchedule.Round round : parsed.getRounds()) {
            matchesTotal += round.getMatches() != null ? round.getMatches().size() : 0;
        }

        int remaining = limit != null ? limit : Integer.MAX_VALUE;
        int matchesReturned = 0;
        List<Map<String, Object>> rounds = new ArrayList<>();

        for (Football24ParsedSchedule.Round round : parsed.getRounds()) {
            if (roundFilter != null && round.getNumber() != roundFilter) {
                continue;
            }
            if (remaining <= 0) {
                break;
            }
            List<Map<String, Object>> matches = new ArrayList<>();
            for (Football24ParsedSchedule.Match match : round.getMatches()) {
                if (remaining <= 0) {
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("homeName", match.getHomeName());
                row.put("awayName", match.getAwayName());
                row.put("utcKickoff", match.getUtcKickoff() != null ? match.getUtcKickoff().toString() : null);
                row.put("status", match.getStatus());
                matches.add(row);
                remaining--;
                matchesReturned++;
            }
            if (!matches.isEmpty()) {
                Map<String, Object> roundMap = new LinkedHashMap<>();
                roundMap.put("number", round.getNumber());
                roundMap.put("rawName", round.getRawName());
                roundMap.put("matches", matches);
                rounds.add(roundMap);
            }
        }

        boolean truncated = matchesReturned < matchesTotal;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("leagueId", leagueId);
        out.put("football24SeasonId", parsed.getSeasonId());
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

    private static Map<String, Object> toMelbetOddsTournamentParsed(long tournamentId, List<MelbetPrematchEvent> events) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MelbetPrematchEvent event : events) {
            Map<String, Object> row = new LinkedHashMap<>();
            // UI copies treeId into Event mode — Melbet uses Digitain eventId.
            row.put("treeId", event.getEventId());
            row.put("eventId", event.getEventId());
            row.put("name", event.getName());
            row.put("homeTeam", event.getHomeTeamEn() != null ? event.getHomeTeamEn() : event.getHomeTeam());
            row.put("awayTeam", event.getAwayTeamEn() != null ? event.getAwayTeamEn() : event.getAwayTeam());
            row.put("displayTimeMillis", event.kickoffEpochMillis());
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "tournament");
        out.put("tournamentTreeId", tournamentId);
        out.put("eventsCount", rows.size());
        out.put("events", rows);
        return out;
    }

    private static Map<String, Object> toMelbetOddsEventParsed(long eventId, JsonNode body) {
        Map<String, Integer> bucketCounts = new LinkedHashMap<>();
        for (MelbetMarketBucket bucket : MelbetMarketBucket.values()) {
            bucketCounts.put(bucket.name(), 0);
        }
        int marketsTotal = 0;
        if (body != null) {
            Iterable<JsonNode> parts = body.isArray() ? body : List.of(body);
            for (JsonNode part : parts) {
                JsonNode stakeTypes = part != null ? part.get("StakeTypes") : null;
                if (stakeTypes == null || !stakeTypes.isArray()) {
                    continue;
                }
                for (JsonNode st : stakeTypes) {
                    if (st == null || !st.hasNonNull("Id")) {
                        continue;
                    }
                    var bucket = MelbetAllowedMarkets.bucketFor(st.get("Id").asInt());
                    if (bucket.isEmpty()) {
                        continue;
                    }
                    marketsTotal++;
                    String key = bucket.get().name();
                    bucketCounts.put(key, bucketCounts.getOrDefault(key, 0) + 1);
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "event");
        out.put("eventTreeId", eventId);
        out.put("marketsTotal", marketsTotal);
        out.put("marketBucketCounts", bucketCounts);
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

    private Map<String, Object> toLiveParsed(
            String date,
            String titleContains,
            int competitionsTotal,
            List<TwentyFourScoreParsedDatePage.CompetitionBlock> filtered,
            String provider
    ) {
        int matchesCount = 0;
        List<Map<String, Object>> competitions = new ArrayList<>();
        for (TwentyFourScoreParsedDatePage.CompetitionBlock block : filtered) {
            List<Map<String, Object>> matches = new ArrayList<>();
            for (TwentyFourScoreParsedDatePage.MatchRow match : block.getMatches()) {
                matchesCount++;
                matches.add(liveMatchRow(
                        provider,
                        match.getExternalMatchId(),
                        match.getHomeName(),
                        match.getAwayName(),
                        match.getScoreText(),
                        match.getFullTimeScore(),
                        match.getFirstTimeScore(),
                        null,
                        match.getLiveMinuteLabel(),
                        match.getStatus()
                ));
            }
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("title", block.getTitle());
            comp.put("matches", matches);
            competitions.add(comp);
        }
        return liveParsedEnvelope(date, titleContains, competitionsTotal, matchesCount, competitions);
    }

    private Map<String, Object> toChampionatLiveParsed(
            String date,
            String titleContains,
            int competitionsTotal,
            List<ChampionatParsedDatePage.CompetitionBlock> filtered,
            String provider
    ) {
        int matchesCount = 0;
        List<Map<String, Object>> competitions = new ArrayList<>();
        for (ChampionatParsedDatePage.CompetitionBlock block : filtered) {
            List<Map<String, Object>> matches = new ArrayList<>();
            for (ChampionatParsedDatePage.MatchRow match : block.getMatches()) {
                matchesCount++;
                LiveMatchSnapshot snapshot = match.getSnapshot();
                matches.add(liveMatchRow(
                        provider,
                        match.getExternalMatchId(),
                        match.getHomeName(),
                        match.getAwayName(),
                        match.getScoreText(),
                        snapshot != null ? snapshot.fullTimeScore() : null,
                        snapshot != null ? snapshot.firstTimeScore() : null,
                        snapshot != null ? snapshot.penaltyScore() : null,
                        snapshot != null ? snapshot.rawMinuteLabel() : null,
                        snapshot != null ? snapshot.status() : null
                ));
            }
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("title", block.getTitle());
            comp.put("tournamentId", block.getTournamentId());
            comp.put("matches", matches);
            competitions.add(comp);
        }
        return liveParsedEnvelope(date, titleContains, competitionsTotal, matchesCount, competitions);
    }

    private Map<String, Object> liveMatchRow(
            String provider,
            String externalMatchId,
            String homeName,
            String awayName,
            String scoreText,
            String fullTimeScore,
            String firstTimeScore,
            String penaltyScore,
            String liveMinuteLabel,
            String status
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("externalMatchId", externalMatchId);
        row.put("homeName", homeName);
        row.put("awayName", awayName);
        row.put("homeTeam", resolveSandboxTeam(provider, homeName));
        row.put("awayTeam", resolveSandboxTeam(provider, awayName));
        row.put("scoreText", scoreText);
        row.put("fullTimeScore", fullTimeScore);
        row.put("firstTimeScore", firstTimeScore);
        row.put("penaltyScore", penaltyScore);
        row.put("liveMinuteLabel", liveMinuteLabel);
        row.put("status", status);
        return row;
    }

    private static Map<String, Object> liveParsedEnvelope(
            String date,
            String titleContains,
            int competitionsTotal,
            int matchesCount,
            List<Map<String, Object>> competitions
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date);
        out.put("titleContains", titleContains.isEmpty() ? null : titleContains);
        out.put("competitionsTotal", competitionsTotal);
        out.put("competitionsMatched", competitions.size());
        out.put("matchesCount", matchesCount);
        out.put("competitions", competitions);
        return out;
    }

    /**
     * Best-effort alias resolve for sandbox card preview. Missing alias → null (UI keeps default logo).
     */
    private Map<String, Object> resolveSandboxTeam(String provider, String externalName) {
        if (externalName == null || externalName.isBlank()) {
            return null;
        }
        return teamAliasResolver.resolveByProviderName(provider, externalName)
                .map(team -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", team.getId());
                    dto.put("title", team.getTitle());
                    dto.put("logoKey", team.getLogo());
                    return dto;
                })
                .orElse(null);
    }

    private static int countNonCardGoals(List<MatchGoalEvent> goals) {
        if (goals == null || goals.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (MatchGoalEvent g : goals) {
            if (g == null || Boolean.TRUE.equals(g.getRedCard()) || Boolean.TRUE.equals(g.getMissed())) {
                continue;
            }
            n++;
        }
        return n;
    }
}
