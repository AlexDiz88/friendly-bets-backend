package net.friendly_bets.sportsru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SportsRuScheduleParser {

    private static final String SPORTS_TEAM_TYPE = "SportsTeam";

    private final ObjectMapper objectMapper;

    public SportsRuScheduleParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** No-arg for unit tests that do not need JSON-LD parsing. */
    SportsRuScheduleParser() {
        this(new ObjectMapper());
    }

    private static final Pattern ROUND_TITLE = Pattern.compile(
            "^\\s*(\\d+)\\s*тур\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SCHEDULED_AT_Z = Pattern.compile(
            "scheduledAt\"?\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\"");
    private static final Pattern JSON_LD_START = Pattern.compile(
            "\"startDate\"\\s*:\\s*\"([^\"]+)\"");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Calendar schedule, tried in order:
     * <ul>
     *   <li>{@code calendar-list} + {@code calendar-card} (EPL/BL/CL — rounds keyed by
     *       {@code calendar-group-header--stage} «N тур»; day headers ignored)</li>
     *   <li>legacy {@code h3}+{@code table.stat-table} (still used by LE / UEL)</li>
     *   <li>Vue {@code match-schedule-column} (older CL/LE layout)</li>
     * </ul>
     * Round number is the primary grouping; kickoff Instant on the card is optional enrichment.
     */
    public SportsRuParsedSchedule parseCalendar(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        List<SportsRuParsedSchedule.Round> calendarList = parseCalendarList(doc);
        if (hasAnyMatches(calendarList)) {
            return SportsRuParsedSchedule.builder().rounds(calendarList).build();
        }
        List<SportsRuParsedSchedule.Round> legacy = parseLegacyCalendar(doc);
        if (hasAnyMatches(legacy)) {
            return SportsRuParsedSchedule.builder().rounds(legacy).build();
        }
        return SportsRuParsedSchedule.builder().rounds(parseVueCalendar(doc)).build();
    }

    /** Team names from a calendar round (legacy stat-table or Vue match-schedule layout). */
    public List<String> parseTeamNamesFromMatchday(String html, int matchday) {
        SportsRuParsedSchedule parsed = parseCalendar(html);
        SportsRuParsedSchedule.Round round = parsed.roundsByNumber().get(matchday);
        if (round == null || round.getMatches() == null || round.getMatches().isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (SportsRuParsedSchedule.Match match : round.getMatches()) {
            addTeamName(names, match.getHomeName());
            addTeamName(names, match.getAwayName());
        }
        return new ArrayList<>(names);
    }

    /** Team names from tournament table page ({@code /table/}, {@code table.stat-table a.name}). */
    public List<String> parseTeamNamesFromTable(String html) {
        Set<String> names = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        for (Element link : doc.select("table.stat-table a.name[href*=/football/club/]")) {
            String name = link.attr("title");
            if (name == null || name.isBlank()) {
                name = link.text();
            }
            addTeamName(names, name);
        }
        return new ArrayList<>(names);
    }

    /** Team names from embedded JSON-LD ({@code SportsTeam} / {@code competitor} on calendar or table pages). */
    public List<String> parseTeamNamesFromJsonLd(String html) {
        Set<String> names = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json = script.data();
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(json);
                collectSportsTeamNames(root, names);
            } catch (Exception ignored) {
                // skip malformed blocks
            }
        }
        return new ArrayList<>(names);
    }

    private void collectSportsTeamNames(JsonNode node, Set<String> names) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectSportsTeamNames(item, names);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode typeNode = node.get("@type");
        if (typeNode != null && SPORTS_TEAM_TYPE.equals(typeNode.asText())) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.isTextual()) {
                addTeamName(names, nameNode.asText());
            }
        }
        node.fields().forEachRemaining(entry -> collectSportsTeamNames(entry.getValue(), names));
    }

    /**
     * New sports.ru calendar (EPL/BL/CL): all tours in one SSR page; UI round selector only filters.
     * Group solely by stage header «N тур» — day headers are not used for matching or kickoff.
     */
    private List<SportsRuParsedSchedule.Round> parseCalendarList(Document doc) {
        Element list = doc.selectFirst("div.calendar-list, [data-testid=calendar-list]");
        if (list == null) {
            return List.of();
        }
        List<SportsRuParsedSchedule.Round> rounds = new ArrayList<>();
        Map<Integer, SportsRuParsedSchedule.Round> byNumber = new LinkedHashMap<>();
        SportsRuParsedSchedule.Round current = null;

        for (Element el : list.getAllElements()) {
            if (el == list) {
                continue;
            }
            if (el.hasClass("calendar-group-header--stage")) {
                Optional<Integer> roundNo = parseRoundNumber(el.text());
                if (roundNo.isEmpty()) {
                    current = null;
                    continue;
                }
                current = byNumber.computeIfAbsent(roundNo.get(), n -> {
                    SportsRuParsedSchedule.Round round = SportsRuParsedSchedule.Round.builder()
                            .number(n)
                            .matches(new ArrayList<>())
                            .build();
                    rounds.add(round);
                    return round;
                });
                continue;
            }
            if (current == null
                    || !"article".equalsIgnoreCase(el.tagName())
                    || !el.hasClass("calendar-card")) {
                continue;
            }
            parseCalendarCard(el).ifPresent(current.getMatches()::add);
        }
        return rounds;
    }

    private Optional<SportsRuParsedSchedule.Match> parseCalendarCard(Element card) {
        Element homeEl = card.selectFirst("a.calendar-card__home, a.calendar-team.calendar-card__home");
        Element awayEl = card.selectFirst("a.calendar-card__away, a.calendar-team.calendar-card__away");
        Element link = card.selectFirst("a.calendar-card__match-link[href]");
        if (homeEl == null || awayEl == null || link == null) {
            return Optional.empty();
        }
        String homeName = resolveCalendarTeamName(homeEl);
        String awayName = resolveCalendarTeamName(awayEl);
        if (homeName.isBlank() || awayName.isBlank()) {
            return Optional.empty();
        }
        String matchPath = normalizeMatchPath(link.attr("href"));
        if (matchPath == null) {
            return Optional.empty();
        }
        Instant utcKickoff = parseUtcKickoffFromDatetimeAttr(
                card.selectFirst("time.calendar-status__moment[datetime]"));
        String status = mapCalendarMatchStatus(card);
        return Optional.of(SportsRuParsedSchedule.Match.builder()
                .homeName(homeName)
                .awayName(awayName)
                .matchPath(matchPath)
                .utcKickoff(utcKickoff)
                .status(status)
                .build());
    }

    /**
     * Prefer full name from profile title; fall back to link own-text / logo alt.
     * Never invent names from match URL slug (home/away order in slug can disagree with card).
     */
    private static String resolveCalendarTeamName(Element teamLink) {
        String title = teamLink.attr("title");
        String profilePrefix = "Перейти в профиль команды ";
        if (title != null && title.startsWith(profilePrefix)) {
            String fromTitle = title.substring(profilePrefix.length()).trim();
            if (!fromTitle.isBlank()) {
                return fromTitle;
            }
        }
        String own = teamLink.ownText();
        if (own != null && !own.isBlank()) {
            return own.trim();
        }
        Element img = teamLink.selectFirst("img[alt]");
        if (img != null) {
            String alt = img.attr("alt");
            String logoPrefix = "Логотип команды ";
            if (alt != null && alt.startsWith(logoPrefix)) {
                String fromAlt = alt.substring(logoPrefix.length()).trim();
                if (!fromAlt.isBlank()) {
                    return fromAlt;
                }
            }
        }
        return teamLink.text().trim();
    }

    private static String mapCalendarMatchStatus(Element card) {
        String raw = card.attr("data-match-status");
        if (raw != null && !raw.isBlank()) {
            String normalized = raw.trim().toUpperCase();
            if ("NOT_STARTED".equals(normalized) || "SCHEDULED".equals(normalized)) {
                return "SCHEDULED";
            }
            if ("FINISHED".equals(normalized) || "ENDED".equals(normalized) || "COMPLETE".equals(normalized)) {
                return "FINISHED";
            }
        }
        Element score = card.selectFirst(".calendar-card__score, .calendar-score");
        if (score != null && score.hasClass("calendar-score__score--empty")) {
            return "SCHEDULED";
        }
        String scoreText = score != null ? score.text() : "";
        return isPlaceholderScore(scoreText) ? "SCHEDULED" : "FINISHED";
    }

    /**
     * Strict Instant only: {@code datetime} with {@code Z} or numeric offset. No wall-clock / TZ guess.
     */
    private static Instant parseUtcKickoffFromDatetimeAttr(Element timeEl) {
        if (timeEl == null) {
            return null;
        }
        String raw = timeEl.attr("datetime");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            if (value.endsWith("Z") || value.endsWith("z")) {
                return Instant.parse(value);
            }
            OffsetDateTime odt = OffsetDateTime.parse(value, ISO_OFFSET);
            return odt.toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private List<SportsRuParsedSchedule.Round> parseLegacyCalendar(Document doc) {
        List<SportsRuParsedSchedule.Round> rounds = new ArrayList<>();
        for (Element heading : doc.select("h3")) {
            Optional<Integer> roundNo = parseRoundNumber(heading.text());
            if (roundNo.isEmpty()) {
                continue;
            }
            SportsRuParsedSchedule.Round current = SportsRuParsedSchedule.Round.builder()
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
        return rounds;
    }

    /**
     * Vue calendar: document order of {@code group-header} / {@code matches-item}.
     * Non-{@code N тур} headers (квалификация, плей-офф) clear the current round.
     */
    private List<SportsRuParsedSchedule.Round> parseVueCalendar(Document doc) {
        List<SportsRuParsedSchedule.Round> rounds = new ArrayList<>();
        Map<Integer, SportsRuParsedSchedule.Round> byNumber = new LinkedHashMap<>();
        SportsRuParsedSchedule.Round current = null;

        for (Element column : doc.select("div.match-schedule-column")) {
            for (Element el : column.getAllElements()) {
                if (el == column) {
                    continue;
                }
                if (el.hasClass("match-schedule-column__group-header")) {
                    Optional<Integer> roundNo = parseRoundNumber(el.text());
                    if (roundNo.isEmpty()) {
                        current = null;
                        continue;
                    }
                    current = byNumber.computeIfAbsent(roundNo.get(), n -> {
                        SportsRuParsedSchedule.Round round = SportsRuParsedSchedule.Round.builder()
                                .number(n)
                                .matches(new ArrayList<>())
                                .build();
                        rounds.add(round);
                        return round;
                    });
                    continue;
                }
                if (current == null || !el.hasClass("match-schedule-column__matches-item")) {
                    continue;
                }
                parseVueMatchTeaser(el).ifPresent(current.getMatches()::add);
            }
        }
        return rounds;
    }

    private Optional<SportsRuParsedSchedule.Match> parseVueMatchTeaser(Element teaser) {
        Element homeEl = teaser.selectFirst(".match-teaser__team--home .match-teaser__team-name");
        Element awayEl = teaser.selectFirst(".match-teaser__team--away .match-teaser__team-name");
        Element link = teaser.selectFirst("a.match-teaser__link[href]");
        if (homeEl == null || awayEl == null || link == null) {
            return Optional.empty();
        }
        String homeName = resolveVueTeamName(homeEl);
        String awayName = resolveVueTeamName(awayEl);
        if (homeName.isBlank() || awayName.isBlank()) {
            return Optional.empty();
        }
        String matchPath = normalizeMatchPath(link.attr("href"));
        if (matchPath == null) {
            return Optional.empty();
        }
        Element scoreEl = teaser.selectFirst(".match-teaser__team-score");
        String scoreText = scoreEl != null ? scoreEl.text() : "";
        String status = isPlaceholderScore(scoreText) ? "SCHEDULED" : "FINISHED";
        return Optional.of(SportsRuParsedSchedule.Match.builder()
                .homeName(homeName)
                .awayName(awayName)
                .matchPath(matchPath)
                .status(status)
                .build());
    }

    /**
     * sports.ru often sets home name via typo attribute {@code tite} instead of {@code title}.
     */
    private static String resolveVueTeamName(Element nameEl) {
        String title = nameEl.attr("title");
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String tite = nameEl.attr("tite");
        if (tite != null && !tite.isBlank()) {
            return tite.trim();
        }
        return nameEl.text().trim();
    }

    private static boolean hasAnyMatches(List<SportsRuParsedSchedule.Round> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return false;
        }
        for (SportsRuParsedSchedule.Round round : rounds) {
            if (round.getMatches() != null && !round.getMatches().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void addTeamName(Set<String> names, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        names.add(raw.trim());
    }

    /**
     * Kickoff from match-page {@code scheduledAt} with {@code Z}, else JSON-LD {@code startDate} with offset.
     * Used only when the calendar card has no strict {@code datetime}. Display wall-clock is never used.
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
        return !normalized.matches(".*\\d+\\s*[:\\-]\\s*\\d+.*");
    }
}
