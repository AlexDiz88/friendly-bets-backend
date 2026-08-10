package net.friendly_bets.ruscore;

import net.friendly_bets.exceptions.BadRequestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuscoreDayPageParser {

    private static final Pattern GAME_HREF = Pattern.compile(
            "^/game/([^/]+)/(\\d+)(?:/summary)?/?$"
    );
    private static final Pattern EVENT_KICKOFF = Pattern.compile(
            "(\\d{5,}),\"(20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))\""
    );
    private static final Pattern TOURNAMENT_HREF = Pattern.compile(
            "^/tournament/([^/]+)/(\\d+)(?:/.*)?$"
    );

    public RuscoreParsedDayPage parse(String html, LocalDate date) {
        if (html == null || html.isBlank()) {
            throw new BadRequestException("ruscoreParseFailed");
        }
        Document doc = Jsoup.parse(html);
        Map<String, Instant> kickoffs = parseKickoffs(html);

        Map<String, CompetitionBucket> byKey = new LinkedHashMap<>();
        String currentTitle = "";
        Integer currentSeasonId = null;
        String currentSlug = null;

        Elements walk = doc.getAllElements();
        for (Element el : walk) {
            String testId = el.attr("data-test-id");
            if ("event-list-header-title-text".equals(testId)) {
                String t = text(el);
                if (t != null) {
                    currentTitle = t;
                }
                TournamentRef ref = findTournamentRef(el);
                if (ref != null) {
                    currentSeasonId = ref.seasonId;
                    currentSlug = ref.slug;
                }
                continue;
            }
            if ("event-list-header".equals(testId)) {
                // Prefer title-text when present; otherwise whole header title (+ country).
                Element titleText = el.selectFirst("[data-test-id=event-list-header-title-text]");
                if (titleText == null) {
                    String t = text(el.selectFirst("[data-test-id=event-list-header-title]"));
                    if (t == null) {
                        t = text(el);
                    }
                    if (t != null) {
                        currentTitle = t;
                    }
                }
                TournamentRef ref = findTournamentRef(el);
                if (ref != null) {
                    currentSeasonId = ref.seasonId;
                    currentSlug = ref.slug;
                }
                continue;
            }
            if (!"a".equals(el.normalName())) {
                continue;
            }
            String href = el.attr("href");
            if (href == null || !href.startsWith("/game/")) {
                continue;
            }
            Matcher hm = GAME_HREF.matcher(href.trim());
            if (!hm.matches()) {
                continue;
            }
            String slug = hm.group(1);
            String eventId = hm.group(2);
            String home = text(el.selectFirst("[data-test-id=player-title-home]"));
            String away = text(el.selectFirst("[data-test-id=player-title-away]"));
            if (home == null || away == null) {
                continue;
            }
            String status = text(el.selectFirst("[data-test-id=event-item-time-status]"));
            if (status == null) {
                status = text(el.selectFirst("[data-test-id=status]"));
            }
            String homeScore = text(el.selectFirst("[data-test-id=event-item-overall-score-home]"));
            String awayScore = text(el.selectFirst("[data-test-id=event-item-overall-score-away]"));
            String scoreText = null;
            if (homeScore != null && awayScore != null && !homeScore.isBlank() && !awayScore.isBlank()) {
                scoreText = homeScore.trim() + ":" + awayScore.trim();
            }
            RuscoreParsedDayPage.Match match = RuscoreParsedDayPage.Match.builder()
                    .eventId(eventId)
                    .slug(slug)
                    .homeName(home)
                    .awayName(away)
                    .utcKickoff(kickoffs.get(eventId))
                    .statusText(status)
                    .scoreText(scoreText)
                    .build();
            final String titleSnapshot = currentTitle.isBlank() ? null : currentTitle;
            final Integer seasonIdSnapshot = currentSeasonId;
            final String slugSnapshot = currentSlug;
            String key = competitionKey(currentTitle, currentSeasonId, currentSlug);
            CompetitionBucket bucket = byKey.computeIfAbsent(
                    key,
                    k -> new CompetitionBucket(titleSnapshot, seasonIdSnapshot, slugSnapshot)
            );
            bucket.matches.add(match);
        }

        List<RuscoreParsedDayPage.CompetitionBlock> competitions = new ArrayList<>();
        for (CompetitionBucket bucket : byKey.values()) {
            competitions.add(RuscoreParsedDayPage.CompetitionBlock.builder()
                    .title(bucket.title)
                    .seasonId(bucket.seasonId)
                    .tournamentSlug(bucket.tournamentSlug)
                    .matches(bucket.matches)
                    .build());
        }
        return RuscoreParsedDayPage.builder()
                .date(date != null ? date.toString() : null)
                .competitions(competitions)
                .build();
    }

    /**
     * True when competition matches sandbox title/id filter (substring on title/slug or exact season id).
     */
    public static boolean competitionMatchesFilter(
            RuscoreParsedDayPage.CompetitionBlock block,
            String titleContainsRaw
    ) {
        if (titleContainsRaw == null || titleContainsRaw.isBlank()) {
            return true;
        }
        if (block == null) {
            return false;
        }
        String needle = titleContainsRaw.trim();
        String needleLower = needle.toLowerCase(Locale.ROOT);
        if (block.getSeasonId() != null && String.valueOf(block.getSeasonId()).equals(needle)) {
            return true;
        }
        if (block.getTitle() != null && block.getTitle().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        if (block.getTournamentSlug() != null
                && block.getTournamentSlug().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        return false;
    }

    static Map<String, Instant> parseKickoffs(String html) {
        Map<String, Instant> map = new HashMap<>();
        if (html == null) {
            return map;
        }
        Matcher m = EVENT_KICKOFF.matcher(html);
        while (m.find()) {
            Instant kickoff = parseInstant(m.group(2));
            if (kickoff != null) {
                map.putIfAbsent(m.group(1), kickoff);
            }
        }
        return map;
    }

    private static TournamentRef findTournamentRef(Element from) {
        if (from == null) {
            return null;
        }
        Element scope = from;
        // Header block may contain the /tournament/{slug}/{seasonId} link.
        for (int i = 0; i < 4 && scope != null; i++) {
            for (Element a : scope.select("a[href^=/tournament/]")) {
                Matcher m = TOURNAMENT_HREF.matcher(a.attr("href").trim());
                if (m.matches()) {
                    try {
                        return new TournamentRef(m.group(1), Integer.parseInt(m.group(2)));
                    } catch (NumberFormatException ignored) {
                        // continue
                    }
                }
            }
            scope = scope.parent();
        }
        return null;
    }

    private static String competitionKey(String title, Integer seasonId, String slug) {
        if (seasonId != null) {
            return "id:" + seasonId;
        }
        if (slug != null && !slug.isBlank()) {
            return "slug:" + slug;
        }
        return "title:" + (title != null ? title : "");
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String text(Element el) {
        if (el == null) {
            return null;
        }
        String t = el.text();
        if (t == null) {
            return null;
        }
        t = t.replace('\u00a0', ' ').trim();
        return t.isEmpty() ? null : t;
    }

    private record TournamentRef(String slug, int seasonId) {
    }

    private static final class CompetitionBucket {
        private final String title;
        private final Integer seasonId;
        private final String tournamentSlug;
        private final List<RuscoreParsedDayPage.Match> matches = new ArrayList<>();

        private CompetitionBucket(String title, Integer seasonId, String tournamentSlug) {
            this.title = title;
            this.seasonId = seasonId;
            this.tournamentSlug = tournamentSlug;
        }
    }
}
