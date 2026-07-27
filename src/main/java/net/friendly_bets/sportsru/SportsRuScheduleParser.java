package net.friendly_bets.sportsru;

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
public class SportsRuScheduleParser {

    private static final Pattern ROUND_TITLE = Pattern.compile(
            "^\\s*(\\d+)\\s*тур\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SCHEDULED_AT_Z = Pattern.compile(
            "scheduledAt\"?\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\"");
    private static final Pattern JSON_LD_START = Pattern.compile(
            "\"startDate\"\\s*:\\s*\"([^\"]+)\"");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public SportsRuParsedSchedule parseCalendar(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        List<SportsRuParsedSchedule.Round> rounds = new ArrayList<>();
        SportsRuParsedSchedule.Round current = null;

        Elements headings = doc.select("h3");
        for (Element heading : headings) {
            Optional<Integer> roundNo = parseRoundNumber(heading.text());
            if (roundNo.isEmpty()) {
                continue;
            }
            current = SportsRuParsedSchedule.Round.builder()
                    .number(roundNo.get())
                    .matches(new ArrayList<>())
                    .build();
            rounds.add(current);

            Element table = findFollowingStatTable(heading);
            if (table == null) {
                continue;
            }
            for (Element row : table.select("tbody > tr")) {
                parseMatchRow(row).ifPresent(current.getMatches()::add);
            }
        }

        return SportsRuParsedSchedule.builder().rounds(rounds).build();
    }

    /** Team names from round 1 of a domestic calendar page. */
    public List<String> parseTeamNamesFromMatchday(String html, int matchday) {
        SportsRuParsedSchedule parsed = parseCalendar(html);
        SportsRuParsedSchedule.Round round = parsed.roundsByNumber().get(matchday);
        if (round == null || round.getMatches() == null || round.getMatches().isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (SportsRuParsedSchedule.Match match : round.getMatches()) {
            if (match.getHomeName() != null && !match.getHomeName().isBlank()) {
                names.add(match.getHomeName().trim());
            }
            if (match.getAwayName() != null && !match.getAwayName().isBlank()) {
                names.add(match.getAwayName().trim());
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Kickoff only from match-page {@code scheduledAt} with {@code Z}, else JSON-LD {@code startDate} with offset.
     * Display wall-clock from the calendar is never used.
     */
    public Instant parseUtcKickoffFromMatchHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher scheduledAt = SCHEDULED_AT_Z.matcher(html);
        if (scheduledAt.find()) {
            try {
                return Instant.parse(scheduledAt.group(1).trim());
            } catch (Exception ignored) {
                // fall through to startDate
            }
        }
        Matcher startDate = JSON_LD_START.matcher(html);
        if (startDate.find()) {
            try {
                OffsetDateTime odt = OffsetDateTime.parse(startDate.group(1).trim(), ISO_OFFSET);
                return odt.toInstant();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
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

    private Element findFollowingStatTable(Element heading) {
        Element sibling = heading.nextElementSibling();
        while (sibling != null) {
            if ("h3".equalsIgnoreCase(sibling.tagName())) {
                return null;
            }
            Element table = sibling.selectFirst("table.stat-table");
            if (table != null) {
                return table;
            }
            if (sibling.hasClass("stat-table") && "table".equalsIgnoreCase(sibling.tagName())) {
                return sibling;
            }
            sibling = sibling.nextElementSibling();
        }
        return null;
    }

    private Optional<SportsRuParsedSchedule.Match> parseMatchRow(Element row) {
        Element homeLink = row.selectFirst("td.owner-td a.player");
        Element awayLink = row.selectFirst("td.guests-td a.player");
        Element scoreLink = row.selectFirst("td.score-td a.score");
        if (homeLink == null || awayLink == null || scoreLink == null) {
            return Optional.empty();
        }
        String homeName = resolveTeamName(homeLink);
        String awayName = resolveTeamName(awayLink);
        if (homeName.isBlank() || awayName.isBlank()) {
            return Optional.empty();
        }
        String matchPath = normalizeMatchPath(scoreLink.attr("href"));
        if (matchPath == null) {
            return Optional.empty();
        }
        String scoreText = scoreLink.text();
        String status = isPlaceholderScore(scoreText) ? "SCHEDULED" : "FINISHED";
        return Optional.of(SportsRuParsedSchedule.Match.builder()
                .homeName(homeName)
                .awayName(awayName)
                .matchPath(matchPath)
                .status(status)
                .build());
    }

    private static String resolveTeamName(Element link) {
        String title = link.attr("title");
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return link.text().trim();
    }

    private static String normalizeMatchPath(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        String path = href.trim();
        int scheme = path.indexOf("://");
        if (scheme >= 0) {
            int slash = path.indexOf('/', scheme + 3);
            if (slash < 0) {
                return null;
            }
            path = path.substring(slash);
        }
        if (!path.startsWith("/football/match/")) {
            return null;
        }
        // Date-only links (/football/match/2026-08-21/) are not match cards.
        if (path.matches("/football/match/\\d{4}-\\d{2}-\\d{2}/?")) {
            return null;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }

    private static boolean isPlaceholderScore(String scoreText) {
        if (scoreText == null || scoreText.isBlank()) {
            return true;
        }
        String normalized = scoreText.replace('\u2013', '-').replace('\u2014', '-').trim();
        return normalized.contains("-") && !normalized.matches(".*\\d+\\s*:\\s*\\d+.*");
    }
}
