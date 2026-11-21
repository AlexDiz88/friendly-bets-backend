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
    /**
     * Pinia store of the page's own match. Sibling stores ({@code football-online-historic-games}:
     * H2H / last games of both clubs) describe other matches and are never a kickoff source.
     */
    private static final Pattern PRIMARY_MATCH_STORE = Pattern.compile(
            "(?<![\\w-])\"?football-online-match\"?\\s*:\\s*\\{");
    /** Group 1 is set only for an inline literal with {@code Z} (not a hoisted payload variable). */
    private static final Pattern SCHEDULED_AT_KEY = Pattern.compile(
            "\"?scheduledAt\"?\\s*:\\s*(?:\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\")?");
    /**
     * Hub payload {@code linkToHub:"/football/match/...",scheduledAt:"...Z"}.
     * Historic H2H list items use {@code id}+{@code link:"/football/match/{numericId}/"} — not linkToHub.
     */
    private static final Pattern LINK_TO_HUB_SCHEDULED_AT = Pattern.compile(
            "linkToHub\"?\\s*:\\s*\"(?:\\\\u002F|/)football(?:\\\\u002F|/)match[^\"]*\""
                    + "\\s*,\\s*scheduledAt\"?\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\"");
    private static final String SPORTS_EVENT_TYPE = "SportsEvent";
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
                .kickoffFromCalendar(utcKickoff != null ? Boolean.TRUE : null)
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
     * Kickoff of the <b>primary</b> match on a sports.ru match hub page.
     * Used only when the calendar card has no strict {@code datetime}. Display wall-clock is never used.
     * <p>
     * Hub pages also embed H2H / last games (cup, friendlies, earlier rounds) with their own
     * {@code scheduledAt} — those are never read. Sources, in order:
     * <ol>
     *   <li>own {@code scheduledAt} (inline literal with {@code Z}) of the {@code football-online-match} store;</li>
     *   <li>else hub {@code linkToHub}+{@code scheduledAt} (not historic numeric {@code link} rows);</li>
     *   <li>else {@code startDate} (with offset) of JSON-LD {@code SportsEvent}.</li>
     * </ol>
     * A source yielding several different instants is ambiguous → {@code null}.
     * Never scans the first global {@code scheduledAt} in the HTML.
     */
    public Instant parseUtcKickoffFromMatchHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Set<Instant> fromStore = parsePrimaryStoreKickoffs(html);
        if (!fromStore.isEmpty()) {
            return fromStore.size() == 1 ? fromStore.iterator().next() : null;
        }
        Instant fromHub = firstGroupInstant(LINK_TO_HUB_SCHEDULED_AT.matcher(html));
        if (fromHub != null) {
            return fromHub;
        }
        Set<Instant> fromJsonLd = parseSportsEventKickoffs(html);
        return fromJsonLd.size() == 1 ? fromJsonLd.iterator().next() : null;
    }

    private static Instant firstGroupInstant(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        try {
            return Instant.parse(matcher.group(1).trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Walks each {@code football-online-match} object and reads only its own {@code scheduledAt} key:
     * nested objects ({@code relatedMatch}) and everything after the closing brace are skipped.
     */
    private static Set<Instant> parsePrimaryStoreKickoffs(String html) {
        Set<Instant> kickoffs = new LinkedHashSet<>();
        Matcher store = PRIMARY_MATCH_STORE.matcher(html);
        Matcher key = SCHEDULED_AT_KEY.matcher(html);
        while (store.find()) {
            int depth = 1;
            boolean keyPosition = true;
            int i = store.end();
            while (i < html.length() && depth > 0) {
                char c = html.charAt(i);
                if (depth == 1 && keyPosition && key.region(i, html.length()).lookingAt()) {
                    if (key.group(1) != null) {
                        try {
                            kickoffs.add(Instant.parse(key.group(1)));
                        } catch (Exception ignored) {
                            // not a valid instant — not a kickoff source
                        }
                    }
                    keyPosition = false;
                    i = key.end();
                    continue;
                }
                if (c == '"') {
                    keyPosition = false;
                    i = endOfStringLiteral(html, i);
                    continue;
                }
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                }
                if (!Character.isWhitespace(c)) {
                    keyPosition = c == ',';
                }
                i++;
            }
        }
        return kickoffs;
    }

    /** Index right after the closing quote of the string literal opened at {@code openQuote}. */
    private static int endOfStringLiteral(String text, int openQuote) {
        int i = openQuote + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return text.length();
    }

    private Set<Instant> parseSportsEventKickoffs(String html) {
        Set<Instant> kickoffs = new LinkedHashSet<>();
        for (Element script : Jsoup.parse(html).select("script[type=application/ld+json]")) {
            String json = script.data();
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                collectSportsEventKickoffs(objectMapper.readTree(json), kickoffs);
            } catch (Exception ignored) {
                // skip malformed blocks
            }
        }
        return kickoffs;
    }

    private static void collectSportsEventKickoffs(JsonNode node, Set<Instant> kickoffs) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectSportsEventKickoffs(item, kickoffs);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode typeNode = node.get("@type");
        JsonNode startDate = node.get("startDate");
        if (typeNode != null && SPORTS_EVENT_TYPE.equals(typeNode.asText())
                && startDate != null && startDate.isTextual()) {
            try {
                kickoffs.add(OffsetDateTime.parse(startDate.asText().trim(), ISO_OFFSET).toInstant());
            } catch (Exception ignored) {
                // no Z / numeric offset — not a strict instant
            }
        }
        node.fields().forEachRemaining(entry -> collectSportsEventKickoffs(entry.getValue(), kickoffs));
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
