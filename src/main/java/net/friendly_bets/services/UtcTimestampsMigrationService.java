package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UtcTimestampsMigrationResultDto;
import net.friendly_bets.utils.UserTimeZones;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot admin migration: normalize timestamp fields to BSON Date (Instant UTC)
 * and backfill {@code accounts.timezone = Europe/Berlin}.
 * <p>
 * Legacy wall-clock values without zone are interpreted as {@code Europe/Berlin}.
 * Existing BSON Date values are left as-is (already Instant UTC).
 */
@Service
@RequiredArgsConstructor
public class UtcTimestampsMigrationService {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static final Map<String, List<String>> COLLECTION_FIELDS = Map.ofEntries(
            Map.entry("match_schedules", List.of(
                    "utc_kickoff", "fetched_at", "finalized_at", "full_details_fetched_at")),
            Map.entry("odds", List.of("fetched_at", "frozen_at")),
            Map.entry("external_api_monitoring", List.of("started_at", "finished_at")),
            Map.entry("error_logs", List.of("created_at")),
            Map.entry("bets", List.of("created_at", "bet_result_added_at", "updated_at")),
            Map.entry("accounts", List.of("created_at")),
            Map.entry("seasons", List.of("created_at")),
            Map.entry("leagues", List.of("created_at")),
            Map.entry("teams", List.of("created_at")),
            Map.entry("calendar_nodes", List.of("created_at")),
            Map.entry("tournament_formats", List.of("created_at")),
            Map.entry("account_tokens", List.of("expires_at", "created_at", "used_at")),
            Map.entry("auth_rate_limits", List.of("window_start")),
            Map.entry("images", List.of("uploadDate")),
            Map.entry("tournament_archives", List.of("exported_at", "imported_at")),
            Map.entry("app_settings", List.of("client_version.updated_at")),
            Map.entry("playoff_brackets", List.of("updated_at")),
            Map.entry("team_standings", List.of("updated_at"))
    );

    private final MongoTemplate mongoTemplate;

    public UtcTimestampsMigrationResultDto migrate() {
        Map<String, UtcTimestampsMigrationResultDto.CollectionStats> stats = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : COLLECTION_FIELDS.entrySet()) {
            String collection = entry.getKey();
            if (!mongoTemplate.collectionExists(collection)) {
                stats.put(collection, UtcTimestampsMigrationResultDto.CollectionStats.builder()
                        .scanned(0).modified(0).build());
                continue;
            }
            long scanned = 0;
            long modified = 0;
            for (Document doc : mongoTemplate.getCollection(collection).find()) {
                scanned++;
                Update update = new Update();
                boolean changed = false;
                for (String fieldPath : entry.getValue()) {
                    Object raw = getNested(doc, fieldPath);
                    Instant instant = toInstant(raw);
                    if (instant == null) {
                        continue;
                    }
                    if (raw instanceof Date) {
                        continue; // already BSON Date / Instant
                    }
                    setNestedUpdate(update, fieldPath, Date.from(instant));
                    changed = true;
                }
                // nested http_logs[].requested_at on monitoring
                if ("external_api_monitoring".equals(collection)) {
                    changed = migrateHttpLogs(doc, update) || changed;
                }
                // nested kickoff_utc on tournament_archives.matches
                if ("tournament_archives".equals(collection)) {
                    changed = migrateArchiveMatches(doc, update) || changed;
                }
                if (changed) {
                    mongoTemplate.updateFirst(
                            Query.query(Criteria.where("_id").is(doc.get("_id"))),
                            update,
                            collection
                    );
                    modified++;
                }
            }
            stats.put(collection, UtcTimestampsMigrationResultDto.CollectionStats.builder()
                    .scanned(scanned)
                    .modified(modified)
                    .build());
        }

        long timezoneBackfilled = backfillAccountTimezones();

        return UtcTimestampsMigrationResultDto.builder()
                .collections(stats)
                .accountsTimezoneBackfilled(timezoneBackfilled)
                .message("utcTimestampsMigrationDone")
                .build();
    }

    private long backfillAccountTimezones() {
        if (!mongoTemplate.collectionExists("accounts")) {
            return 0;
        }
        Update update = new Update().set("timezone", UserTimeZones.DEFAULT_TIMEZONE);
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("timezone").exists(false),
                Criteria.where("timezone").is(null),
                Criteria.where("timezone").is("")
        ));
        return mongoTemplate.updateMulti(query, update, "accounts").getModifiedCount();
    }

    private boolean migrateHttpLogs(Document doc, Update update) {
        Object logsObj = doc.get("http_logs");
        if (!(logsObj instanceof List<?> logs) || logs.isEmpty()) {
            return false;
        }
        boolean changed = false;
        List<Document> next = new java.util.ArrayList<>();
        for (Object item : logs) {
            if (!(item instanceof Document log)) {
                continue;
            }
            Document copy = new Document(log);
            Object raw = copy.get("requested_at");
            Instant instant = toInstant(raw);
            if (instant != null && !(raw instanceof Date)) {
                copy.put("requested_at", Date.from(instant));
                changed = true;
            }
            next.add(copy);
        }
        if (changed) {
            update.set("http_logs", next);
        }
        return changed;
    }

    private boolean migrateArchiveMatches(Document doc, Update update) {
        Object matchesObj = doc.get("matches");
        if (!(matchesObj instanceof List<?> matches) || matches.isEmpty()) {
            return false;
        }
        boolean changed = false;
        List<Document> next = new java.util.ArrayList<>();
        for (Object item : matches) {
            if (!(item instanceof Document match)) {
                continue;
            }
            Document copy = new Document(match);
            Object raw = copy.get("kickoff_utc");
            Instant instant = toInstant(raw);
            if (instant != null && !(raw instanceof Date)) {
                copy.put("kickoff_utc", Date.from(instant));
                changed = true;
            }
            next.add(copy);
        }
        if (changed) {
            update.set("matches", next);
        }
        return changed;
    }

    private static Object getNested(Document doc, String path) {
        String[] parts = path.split("\\.");
        Object cur = doc;
        for (String part : parts) {
            if (!(cur instanceof Document d)) {
                return null;
            }
            cur = d.get(part);
        }
        return cur;
    }

    private static void setNestedUpdate(Update update, String path, Object value) {
        update.set(path, value);
    }

    /**
     * BSON Date → Instant as-is.
     * LocalDateTime / Document date parts → interpret as Europe/Berlin wall-clock.
     */
    static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(BERLIN).toInstant();
        }
        if (value instanceof Document doc) {
            Integer year = asInt(doc.get("year"));
            Integer month = asInt(doc.get("monthValue") != null ? doc.get("monthValue") : doc.get("month"));
            Integer day = asInt(doc.get("dayOfMonth") != null ? doc.get("dayOfMonth") : doc.get("day"));
            Integer hour = asInt(doc.get("hour"));
            Integer minute = asInt(doc.get("minute"));
            Integer second = asInt(doc.get("second"));
            Integer nano = asInt(doc.get("nano"));
            if (year != null && month != null && day != null) {
                LocalDateTime ldt = LocalDateTime.of(
                        year,
                        month,
                        day,
                        hour != null ? hour : 0,
                        minute != null ? minute : 0,
                        second != null ? second : 0,
                        nano != null ? nano : 0
                );
                return ldt.atZone(BERLIN).toInstant();
            }
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                if (s.endsWith("Z") || s.contains("+") || s.lastIndexOf('-') > 10) {
                    return Instant.parse(s);
                }
                return LocalDateTime.parse(s).atZone(BERLIN).toInstant();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }
}
