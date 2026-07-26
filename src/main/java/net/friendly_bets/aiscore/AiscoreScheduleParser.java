package net.friendly_bets.aiscore;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses aiscore tournament schedule HTML. Kickoffs are unix-epoch UTC from
 * {@code window.__NUXT__} (not SSR display wall-clock).
 *
 * <p>Match rows are minified as {@code dC[n]={...}} (array name may change);
 * team display names often live in separate {@code {id,name,slug}} embeds and
 * are joined by team id.
 */
@Component
public class AiscoreScheduleParser {

    private static final Pattern NUXT_ASSIGN = Pattern.compile(
            "window\\.__NUXT__\\s*=\\s*(.*?);\\s*</script>",
            Pattern.DOTALL
    );
    /** Any sparse array assignment; filtered by matchTime/homeTeam in object body. */
    private static final Pattern ARRAY_ASSIGN = Pattern.compile("([A-Za-z_$][\\w$]*)\\[(\\d+)]=");
    private static final Pattern TEAM_CATALOG = Pattern.compile(
            "\\{id:([A-Za-z_$][\\w$]*),name:([A-Za-z_$][\\w$]*),slug:([A-Za-z_$][\\w$]*)}"
    );
    private static final Pattern TEAM_EMBED = Pattern.compile(
            "(?:homeTeam|awayTeam):\\{id:([^,}]+)(?:,name:([^,}]+))?(?:,slug:([^,}]+))?}"
    );
    private static final Pattern MATCH_ID = Pattern.compile("\\bid:\"([^\"]+)\"");
    private static final Pattern FIELD = Pattern.compile(
            "(?<![A-Za-z0-9_])(matchTime|roundNum|statusId):([^,}]+)"
    );
    private static final Pattern HOME_TEAM = Pattern.compile(
            "homeTeam:\\{id:([^,}]+)(?:,name:([^,}]+))?(?:,slug:([^,}]+))?}"
    );
    private static final Pattern AWAY_TEAM = Pattern.compile(
            "awayTeam:\\{id:([^,}]+)(?:,name:([^,}]+))?(?:,slug:([^,}]+))?}"
    );

    public AiscoreParsedSchedule parse(String html, String tournamentPath) {
        String nuxt = extractNuxt(html);
        if (nuxt == null || nuxt.isBlank()) {
            return AiscoreParsedSchedule.builder()
                    .tournamentPath(tournamentPath)
                    .rounds(List.of())
                    .build();
        }
        NuxtPayload payload = decodeNuxt(nuxt);
        Map<String, String> teamNames = buildTeamCatalog(payload.body(), payload.env());

        Map<Integer, List<AiscoreParsedSchedule.Match>> grouped = new LinkedHashMap<>();
        Matcher assign = ARRAY_ASSIGN.matcher(payload.body());
        List<Integer> starts = new ArrayList<>();
        while (assign.find()) {
            starts.add(assign.end());
        }
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = i + 1 < starts.size() ? starts.get(i + 1) : payload.body().length();
            String region = payload.body().substring(from, to);
            String obj = extractObjectBody(region.startsWith("{") ? region : "{" + region);
            if (obj == null || !obj.contains("matchTime:") || !obj.contains("homeTeam:")) {
                continue;
            }
            RawMatch raw = parseRawMatch(obj, payload.env(), teamNames);
            if (raw == null || raw.roundNum == null || raw.roundNum < 1
                    || raw.homeName == null || raw.awayName == null) {
                continue;
            }
            grouped.computeIfAbsent(raw.roundNum, n -> new ArrayList<>()).add(
                    AiscoreParsedSchedule.Match.builder()
                            .homeName(raw.homeName)
                            .awayName(raw.awayName)
                            .utcKickoff(raw.utcKickoff)
                            .status("SCHEDULED")
                            .aiscoreMatchId(raw.matchId)
                            .homeTeamId(raw.homeTeamId)
                            .awayTeamId(raw.awayTeamId)
                            .build()
            );
        }

        List<AiscoreParsedSchedule.Round> rounds = new ArrayList<>();
        for (Map.Entry<Integer, List<AiscoreParsedSchedule.Match>> e : grouped.entrySet()) {
            rounds.add(AiscoreParsedSchedule.Round.builder()
                    .number(e.getKey())
                    .matches(e.getValue())
                    .build());
        }
        rounds.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));

        return AiscoreParsedSchedule.builder()
                .tournamentPath(tournamentPath)
                .rounds(rounds)
                .build();
    }

    public List<String> parseAllTeamNames(String html, String tournamentPath) {
        AiscoreParsedSchedule parsed = parse(html, tournamentPath);
        LinkedHashMap<String, Boolean> names = new LinkedHashMap<>();
        for (AiscoreParsedSchedule.Round round : parsed.getRounds()) {
            for (AiscoreParsedSchedule.Match match : round.getMatches()) {
                if (match.getHomeName() != null && !match.getHomeName().isBlank()) {
                    names.put(match.getHomeName().trim(), Boolean.TRUE);
                }
                if (match.getAwayName() != null && !match.getAwayName().isBlank()) {
                    names.put(match.getAwayName().trim(), Boolean.TRUE);
                }
            }
        }
        return new ArrayList<>(names.keySet());
    }

    private static String extractNuxt(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher m = NUXT_ASSIGN.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private static NuxtPayload decodeNuxt(String nuxt) {
        if (!nuxt.startsWith("(function(")) {
            throw new IllegalArgumentException("aiscoreNuxtPayloadInvalid");
        }
        int paramEnd = nuxt.indexOf("){");
        if (paramEnd < 0) {
            throw new IllegalArgumentException("aiscoreNuxtPayloadInvalid");
        }
        String[] params = nuxt.substring("(function(".length(), paramEnd).split(",");
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }
        int braceStart = paramEnd + 1;
        int depth = 0;
        int bodyEnd = -1;
        for (int i = braceStart; i < nuxt.length(); i++) {
            char c = nuxt.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    bodyEnd = i;
                    break;
                }
            }
        }
        if (bodyEnd < 0 || bodyEnd + 2 >= nuxt.length() || nuxt.charAt(bodyEnd + 1) != '(') {
            throw new IllegalArgumentException("aiscoreNuxtPayloadInvalid");
        }
        String body = nuxt.substring(braceStart + 1, bodyEnd);
        String argsRaw = nuxt.substring(bodyEnd + 2, nuxt.length() - 2);
        List<Object> args = parseJsArgs(argsRaw);
        if (args.size() != params.length) {
            throw new IllegalArgumentException("aiscoreNuxtPayloadInvalid");
        }
        Map<String, Object> env = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            env.put(params[i], args.get(i));
        }
        return new NuxtPayload(body, env);
    }

    private static Map<String, String> buildTeamCatalog(String body, Map<String, Object> env) {
        Map<String, String> teams = new HashMap<>();
        Matcher catalog = TEAM_CATALOG.matcher(body);
        while (catalog.find()) {
            Object idObj = resolve(catalog.group(1), env);
            Object nameObj = resolve(catalog.group(2), env);
            if (idObj instanceof String id && nameObj instanceof String name && !name.isBlank()) {
                teams.put(id, name.trim());
            }
        }
        Matcher embedded = TEAM_EMBED.matcher(body);
        while (embedded.find()) {
            Object idObj = resolve(embedded.group(1), env);
            Object nameObj = embedded.group(2) != null ? resolve(embedded.group(2), env) : null;
            if (idObj instanceof String id && nameObj instanceof String name && !name.isBlank()) {
                teams.put(id, name.trim());
            }
        }
        return teams;
    }

    private static RawMatch parseRawMatch(String obj, Map<String, Object> env, Map<String, String> teamNames) {
        Integer roundNum = null;
        Instant utcKickoff = null;
        Integer statusId = null;
        Matcher fields = FIELD.matcher(obj);
        while (fields.find()) {
            String name = fields.group(1);
            Object value = resolve(fields.group(2), env);
            if ("roundNum".equals(name) && value instanceof Number n) {
                roundNum = n.intValue();
            } else if ("matchTime".equals(name) && value instanceof Number n) {
                long epoch = n.longValue();
                if (epoch > 10_000_000_000L) {
                    epoch = epoch / 1000L;
                }
                utcKickoff = Instant.ofEpochSecond(epoch);
            } else if ("statusId".equals(name) && value instanceof Number n) {
                statusId = n.intValue();
            }
        }
        Matcher idM = MATCH_ID.matcher(obj);
        String matchId = idM.find() ? idM.group(1) : null;

        TeamSide home = parseTeamSide(HOME_TEAM.matcher(obj), env, teamNames);
        TeamSide away = parseTeamSide(AWAY_TEAM.matcher(obj), env, teamNames);
        if (home == null || away == null || home.name == null || away.name == null) {
            return null;
        }
        RawMatch raw = new RawMatch();
        raw.roundNum = roundNum;
        raw.utcKickoff = utcKickoff;
        raw.statusId = statusId;
        raw.matchId = matchId;
        raw.homeName = home.name;
        raw.awayName = away.name;
        raw.homeTeamId = home.id;
        raw.awayTeamId = away.id;
        return raw;
    }

    private static TeamSide parseTeamSide(Matcher m, Map<String, Object> env, Map<String, String> teamNames) {
        if (!m.find()) {
            return null;
        }
        Object idObj = resolve(m.group(1), env);
        Object nameObj = m.group(2) != null ? resolve(m.group(2), env) : null;
        String id = idObj instanceof String s ? s : null;
        String name = nameObj instanceof String s && !s.isBlank() ? s.trim() : null;
        if (name == null && id != null) {
            name = teamNames.get(id);
        }
        TeamSide side = new TeamSide();
        side.id = id;
        side.name = name;
        return side;
    }

    private static String extractObjectBody(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start + 1, i);
                }
            }
        }
        return null;
    }

    private static Object resolve(String token, Map<String, Object> env) {
        if (token == null) {
            return null;
        }
        String t = token.trim();
        if (env.containsKey(t)) {
            return env.get(t);
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1);
        }
        if (t.matches("-?\\d+")) {
            return Long.parseLong(t);
        }
        if ("true".equals(t)) {
            return true;
        }
        if ("false".equals(t)) {
            return false;
        }
        if ("null".equals(t)) {
            return null;
        }
        return t;
    }

    private static List<Object> parseJsArgs(String s) {
        List<Object> out = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
            if (i >= n) {
                break;
            }
            ParseResult pr = parseJsValue(s, i);
            out.add(pr.value);
            i = pr.nextIndex;
        }
        return out;
    }

    private static ParseResult parseJsValue(String s, int i) {
        int n = s.length();
        while (i < n && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        if (i >= n) {
            return new ParseResult(null, i);
        }
        char ch = s.charAt(i);
        if (ch == '"' || ch == '\'') {
            char quote = ch;
            i++;
            StringBuilder buf = new StringBuilder();
            while (i < n) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < n) {
                    buf.append(s.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == quote) {
                    i++;
                    break;
                }
                buf.append(c);
                i++;
            }
            return new ParseResult(buf.toString(), i);
        }
        if (s.startsWith("Array(", i)) {
            i += "Array(".length();
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            int j = i;
            while (j < n && Character.isDigit(s.charAt(j))) {
                j++;
            }
            int size = j > i ? Integer.parseInt(s.substring(i, j)) : 0;
            i = j;
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < n && s.charAt(i) == ')') {
                i++;
            }
            List<Object> arr = new ArrayList<>(Math.max(size, 0));
            for (int k = 0; k < size; k++) {
                arr.add(null);
            }
            return new ParseResult(arr, i);
        }
        if (ch == '{' || ch == '[') {
            char open = ch;
            char close = ch == '{' ? '}' : ']';
            int depth = 0;
            int start = i;
            while (i < n) {
                char c = s.charAt(i);
                if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    i++;
                    if (depth == 0) {
                        break;
                    }
                    continue;
                }
                i++;
            }
            return new ParseResult(Map.of("_raw", s.substring(start, i)), i);
        }
        if (Character.isDigit(ch) || (ch == '-' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
            int j = i + 1;
            while (j < n) {
                char c = s.charAt(j);
                if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    j++;
                } else {
                    break;
                }
            }
            String num = s.substring(i, j);
            Object value = num.contains(".") || num.toLowerCase().contains("e")
                    ? Double.parseDouble(num)
                    : Long.parseLong(num);
            return new ParseResult(value, j);
        }
        if (s.startsWith("true", i)) {
            return new ParseResult(true, i + 4);
        }
        if (s.startsWith("false", i)) {
            return new ParseResult(false, i + 5);
        }
        if (s.startsWith("null", i)) {
            return new ParseResult(null, i + 4);
        }
        if (s.startsWith("void 0", i)) {
            return new ParseResult(null, i + 6);
        }
        if (s.startsWith("undefined", i)) {
            return new ParseResult(null, i + 9);
        }
        throw new IllegalArgumentException("aiscoreNuxtPayloadInvalid");
    }

    private record NuxtPayload(String body, Map<String, Object> env) {
    }

    private record ParseResult(Object value, int nextIndex) {
    }

    private static final class RawMatch {
        Integer roundNum;
        Instant utcKickoff;
        Integer statusId;
        String matchId;
        String homeName;
        String awayName;
        String homeTeamId;
        String awayTeamId;
    }

    private static final class TeamSide {
        String id;
        String name;
    }
}
