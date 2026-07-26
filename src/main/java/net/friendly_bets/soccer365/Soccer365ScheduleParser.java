package net.friendly_bets.soccer365;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Soccer365ScheduleParser {

    private static final Pattern ROUND_TITLE = Pattern.compile("(\\d+)\\s*-\\s*й\\s*тур", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern JSON_LD_START = Pattern.compile("\"startDate\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CLUB_FILTER = Pattern.compile(
            "filtersData\\('club','(\\d+)'\\);\"?>([^<]+)</a>",
            Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public Soccer365ParsedSchedule parse(String html, int competitionId) {
        Document doc = Jsoup.parse(html != null ? html : "");
        List<Soccer365ParsedSchedule.Round> rounds = new ArrayList<>();
        Soccer365ParsedSchedule.Round current = null;

        Elements nodes = doc.select(".cmp_stg_ttl, .game_block");
        for (Element el : nodes) {
            if (el.hasClass("cmp_stg_ttl")) {
                Optional<Integer> roundNo = parseRoundNumber(el.text());
                if (roundNo.isEmpty()) {
                    current = null;
                    continue;
                }
                current = Soccer365ParsedSchedule.Round.builder()
                        .number(roundNo.get())
                        .matches(new ArrayList<>())
                        .build();
                rounds.add(current);
                continue;
            }
            if (current == null || !el.hasClass("game_block")) {
                continue;
            }
            parseMatch(el).ifPresent(current.getMatches()::add);
        }

        return Soccer365ParsedSchedule.builder()
                .competitionId(competitionId)
                .rounds(rounds)
                .clubFilterNames(parseClubFilterNames(html))
                .build();
    }

    public List<String> parseClubFilterNames(String html) {
        Set<String> names = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Matcher matcher = CLUB_FILTER.matcher(html);
        while (matcher.find()) {
            String name = matcher.group(2).trim();
            if (!name.isEmpty() && !"Все команды".equalsIgnoreCase(name)) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    /** Names from schedule game_blocks for matchday (not club-filter labels). */
    public List<String> parseTeamNamesFromMatchday(String html, int competitionId, int matchday) {
        Soccer365ParsedSchedule parsed = parse(html, competitionId);
        Soccer365ParsedSchedule.Round round = parsed.roundsByNumber().get(matchday);
        if (round == null || round.getMatches() == null || round.getMatches().isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (Soccer365ParsedSchedule.Match match : round.getMatches()) {
            if (match.getHomeName() != null && !match.getHomeName().isBlank()) {
                names.add(match.getHomeName().trim());
            }
            if (match.getAwayName() != null && !match.getAwayName().isBlank()) {
                names.add(match.getAwayName().trim());
            }
        }
        return new ArrayList<>(names);
    }

    private Optional<Integer> parseRoundNumber(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = ROUND_TITLE.matcher(title.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Soccer365ParsedSchedule.Match> parseMatch(Element gameBlock) {
        String homeName = textOf(gameBlock.selectFirst(".ht .name span"));
        String awayName = textOf(gameBlock.selectFirst(".at .name span"));
        if (homeName.isBlank() || awayName.isBlank()) {
            return Optional.empty();
        }
        Instant utcKickoff = resolveUtcKickoff(gameBlock);
        String homeGoals = textOf(gameBlock.selectFirst(".ht .gls"));
        String awayGoals = textOf(gameBlock.selectFirst(".at .gls"));
        String status = isPlaceholderScore(homeGoals) && isPlaceholderScore(awayGoals)
                ? "SCHEDULED"
                : "FINISHED";
        return Optional.of(Soccer365ParsedSchedule.Match.builder()
                .homeName(homeName)
                .awayName(awayName)
                .utcKickoff(utcKickoff)
                .status(status)
                .soccer365GameId(resolveSoccer365GameId(gameBlock))
                .build());
    }

    private static String resolveSoccer365GameId(Element gameBlock) {
        Element link = gameBlock.selectFirst("a.game_link[dt-id]");
        if (link != null) {
            String dtId = link.attr("dt-id");
            if (dtId != null && !dtId.isBlank()) {
                return dtId.trim();
            }
        }
        Element any = gameBlock.selectFirst("[dt-id]");
        if (any != null) {
            String dtId = any.attr("dt-id");
            if (dtId != null && !dtId.isBlank()) {
                return dtId.trim();
            }
        }
        Element href = gameBlock.selectFirst("a.game_link[href*=/games/]");
        if (href != null) {
            String path = href.attr("href");
            Matcher matcher = Pattern.compile("/games/(\\d+)").matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Kickoff only from JSON-LD {@code startDate} with an explicit offset (or Z) → Instant UTC.
     * Display wall-clock (e.g. {@code 21.08, 21:00}) is never interpreted — no Moscow/Berlin/guess fallback.
     */
    private Instant resolveUtcKickoff(Element gameBlock) {
        Element script = gameBlock.selectFirst("script[type=application/ld+json]");
        if (script == null) {
            return null;
        }
        Matcher matcher = JSON_LD_START.matcher(script.html());
        if (!matcher.find()) {
            return null;
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(matcher.group(1).trim(), ISO_OFFSET);
            return odt.toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPlaceholderScore(String goals) {
        return goals == null || goals.isBlank() || "-".equals(goals) || "–".equals(goals);
    }

    private static String textOf(Element el) {
        return el == null ? "" : el.text().trim();
    }
}
