package net.friendly_bets.flashscore;

import net.friendly_bets.exceptions.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FlashscoreDayFeedParser {

    public FlashscoreParsedDayPage parse(String feed, LocalDate date) {
        if (feed == null || feed.isBlank()) {
            throw new BadRequestException("flashscoreParseFailed");
        }
        Map<String, CompetitionBucket> byKey = new LinkedHashMap<>();
        String currentTitle = null;
        String currentStageId = null;
        String currentPath = null;

        for (String record : FlashscoreFeedSupport.splitRecords(feed)) {
            Map<String, String> fields = FlashscoreFeedSupport.parseRecord(record);
            String za = fields.get("ZA");
            if (za != null) {
                currentTitle = za;
                currentStageId = fields.get("ZC");
                currentPath = fields.get("ZL");
                continue;
            }
            String eventId = fields.get("AA");
            if (eventId == null || eventId.isBlank()) {
                continue;
            }
            // Prefer FH/FK (canonical) over CX/AF (may be localized on ru-kz feeds).
            String home = FlashscoreFeedSupport.firstNonBlank(fields, "FH", "AE", "CX");
            String away = FlashscoreFeedSupport.firstNonBlank(fields, "FK", "AF");
            if (home == null || away == null) {
                continue;
            }
            Instant kickoff = parseKickoff(fields.get("AD"));
            String status = mapStatus(fields.get("AC"));
            String scoreText = formatScore(
                    fields.get("AG"),
                    fields.get("AH"),
                    fields.get("AT"),
                    fields.get("AU")
            );
            FlashscoreParsedDayPage.Match match = FlashscoreParsedDayPage.Match.builder()
                    .eventId(eventId)
                    .homeName(home)
                    .awayName(away)
                    .homeParticipantId(fields.get("PX"))
                    .awayParticipantId(fields.get("PY"))
                    .utcKickoff(kickoff)
                    .statusText(status)
                    .scoreText(scoreText)
                    .build();
            final String titleSnapshot = currentTitle;
            final String stageIdSnapshot = currentStageId;
            final String pathSnapshot = currentPath;
            String key = competitionKey(currentTitle, currentStageId);
            CompetitionBucket bucket = byKey.computeIfAbsent(
                    key,
                    k -> new CompetitionBucket(titleSnapshot, stageIdSnapshot, pathSnapshot)
            );
            bucket.matches.add(match);
        }

        List<FlashscoreParsedDayPage.CompetitionBlock> competitions = new ArrayList<>();
        for (CompetitionBucket bucket : byKey.values()) {
            competitions.add(FlashscoreParsedDayPage.CompetitionBlock.builder()
                    .title(bucket.title)
                    .stageId(bucket.stageId)
                    .tournamentPath(bucket.tournamentPath)
                    .matches(bucket.matches)
                    .build());
        }
        return FlashscoreParsedDayPage.builder()
                .date(date != null ? date.toString() : null)
                .competitions(competitions)
                .build();
    }

    /**
     * True when competition matches sandbox title/id filter (substring on title/path/stage id).
     */
    public static boolean competitionMatchesFilter(
            FlashscoreParsedDayPage.CompetitionBlock block,
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
        if (block.getStageId() != null && block.getStageId().equalsIgnoreCase(needle)) {
            return true;
        }
        if (block.getTitle() != null && block.getTitle().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        if (block.getTournamentPath() != null
                && block.getTournamentPath().toLowerCase(Locale.ROOT).contains(needleLower)) {
            return true;
        }
        return false;
    }

    static Instant parseKickoff(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long epoch = Long.parseLong(raw.trim());
            return Instant.ofEpochSecond(epoch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String mapStatus(String ac) {
        if (ac == null || ac.isBlank()) {
            return null;
        }
        return switch (ac.trim()) {
            case "1" -> "scheduled";
            case "2" -> "live";
            case "3" -> "finished";
            default -> ac;
        };
    }

    private static String formatScore(String home, String away, String homeAlt, String awayAlt) {
        String h = firstNumeric(home, homeAlt);
        String a = firstNumeric(away, awayAlt);
        if (h == null || a == null) {
            return null;
        }
        return h + ":" + a;
    }

    private static String firstNumeric(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private static String competitionKey(String title, String stageId) {
        if (stageId != null && !stageId.isBlank()) {
            return "stage:" + stageId;
        }
        return "title:" + (title != null ? title : "");
    }

    private static final class CompetitionBucket {
        private final String title;
        private final String stageId;
        private final String tournamentPath;
        private final List<FlashscoreParsedDayPage.Match> matches = new ArrayList<>();

        private CompetitionBucket(String title, String stageId, String tournamentPath) {
            this.title = title;
            this.stageId = stageId;
            this.tournamentPath = tournamentPath;
        }
    }
}
