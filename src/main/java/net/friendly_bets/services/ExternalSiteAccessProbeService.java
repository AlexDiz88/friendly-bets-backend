package net.friendly_bets.services;

import net.friendly_bets.dto.ExternalSiteAccessProbeResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.scrape.BrowserProfile;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pre-integration smoke check: can this JVM/host fetch a candidate external site
 * without Cloudflare JS challenge or an SSO interstitial (SberID on championat.com).
 */
@Service
public class ExternalSiteAccessProbeService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSiteAccessProbeService.class);
    private static final int TEXT_SNIPPET_LIMIT = 80_000;
    private static final String MATCH_ROW_SELECTORS =
            "[class*=match-row], [class*=MatchRow], [class*=fixture], [class*=live-score]";

    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(Duration.ofSeconds(15));
    private final BrowserProfile browserProfile = BrowserProfile.randomDesktopRu();

    public ExternalSiteAccessProbeResultDto probe(String urlRaw) {
        URI uri = parseAndValidateUrl(urlRaw);
        String requested = uri.toString();
        long started = System.currentTimeMillis();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(30))
                    .GET();
            ScrapeHttpSupport.applyNavigationHeaders(builder, browserProfile, null);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - started;
            String body = response.body() != null ? response.body() : "";
            String server = firstHeader(response, "server");
            String cfRay = firstHeader(response, "cf-ray");
            String cfMitigated = firstHeader(response, "cf-mitigated");
            boolean cloudflare = isCloudflare(server, cfRay, cfMitigated, body);
            boolean jsChallenge = ScrapeHttpSupport.looksLikeJsChallenge(body);
            boolean authInterstitial = ScrapeHttpSupport.looksLikeAuthInterstitial(body);
            String verdict = resolveVerdict(response.statusCode(), jsChallenge, authInterstitial, body);
            return ExternalSiteAccessProbeResultDto.builder()
                    .verdict(verdict)
                    .requestedUrl(requested)
                    .finalUrl(response.uri() != null ? response.uri().toString() : requested)
                    .httpStatus(response.statusCode())
                    .durationMs(durationMs)
                    .serverHeader(server)
                    .cfRay(cfRay)
                    .cfMitigated(cfMitigated)
                    .cloudflareDetected(cloudflare)
                    .jsChallengeSuspected(jsChallenge || authInterstitial)
                    .bodyLength(body.length())
                    .bodySnippet(snippet(body))
                    .build();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.info("site access probe network error for {}: {}", requested, e.getMessage());
            return ExternalSiteAccessProbeResultDto.builder()
                    .verdict("NETWORK_ERROR")
                    .requestedUrl(requested)
                    .durationMs(System.currentTimeMillis() - started)
                    .cloudflareDetected(false)
                    .jsChallengeSuspected(false)
                    .errorDetail(e.getMessage())
                    .build();
        }
    }

    private static String resolveVerdict(
            int status,
            boolean jsChallenge,
            boolean authInterstitial,
            String body
    ) {
        if (authInterstitial) {
            return "AUTH_INTERSTITIAL";
        }
        if (jsChallenge) {
            return "CLOUDFLARE_JS_CHALLENGE";
        }
        if (looksLikeGraphqlEmptyQuery(status, body)) {
            return "GRAPHQL_NEEDS_QUERY";
        }
        if (status >= 400) {
            return "HTTP_BLOCKED";
        }
        return "PASS";
    }

    static boolean looksLikeGraphqlEmptyQuery(int status, String body) {
        if (status != 400 || body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("no queries to execute")
                || lower.contains("must provide query string")
                || lower.contains("query is missing");
    }

    private static boolean isCloudflare(String server, String cfRay, String cfMitigated, String body) {
        if (cfRay != null && !cfRay.isBlank()) {
            return true;
        }
        if (cfMitigated != null && !cfMitigated.isBlank()) {
            return true;
        }
        if (server != null && server.toLowerCase(Locale.ROOT).contains("cloudflare")) {
            return true;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("cdn-cgi/") || lower.contains("cloudflare");
    }

    private static String firstHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    /**
     * Human-readable extract: JSON as compact text, HTML as visible strings (tags/scripts stripped).
     * Prefer match-row-like blocks when present.
     */
    static String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String trimmed = body.trim();
        if (looksLikeJson(trimmed)) {
            return truncate(trimmed.replaceAll("\\s+", " ").trim());
        }
        if (!looksLikeHtml(trimmed)) {
            return truncate(trimmed.replaceAll("\\s+", " ").trim());
        }
        Document doc = Jsoup.parse(body);
        doc.select("script, style, noscript, svg, iframe").remove();
        String rows = matchRowText(doc);
        if (!rows.isBlank()) {
            return truncate(rows);
        }
        Element root = doc.body() != null ? doc.body() : doc;
        return truncate(root.text());
    }

    private static String matchRowText(Document doc) {
        Elements nodes = doc.select(MATCH_ROW_SELECTORS);
        if (nodes.isEmpty()) {
            return "";
        }
        Set<String> lines = new LinkedHashSet<>();
        for (Element el : nodes) {
            if (hasMatchingAncestor(el)) {
                continue;
            }
            String line = el.text();
            if (line == null) {
                continue;
            }
            line = line.replaceAll("\\s+", " ").trim();
            if (line.length() >= 4) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return "";
        }
        return String.join("\n", lines);
    }

    private static boolean hasMatchingAncestor(Element el) {
        for (Element parent : el.parents()) {
            if (parent.is(MATCH_ROW_SELECTORS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeJson(String trimmed) {
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private static boolean looksLikeHtml(String trimmed) {
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("<!doctype") || lower.startsWith("<html") || lower.contains("<div");
    }

    private static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= TEXT_SNIPPET_LIMIT) {
            return text;
        }
        return text.substring(0, TEXT_SNIPPET_LIMIT) + "…";
    }

    private URI parseAndValidateUrl(String urlRaw) {
        if (urlRaw == null || urlRaw.isBlank()) {
            throw new BadRequestException("siteAccessProbeUrlRequired");
        }
        String trimmed = urlRaw.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("siteAccessProbeUrlInvalid");
        }
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new BadRequestException("siteAccessProbeUrlInvalid");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("siteAccessProbeUrlInvalid");
        }
        if (isDisallowedHost(host)) {
            throw new BadRequestException("siteAccessProbeUrlNotAllowed");
        }
        return uri;
    }

    private static boolean isDisallowedHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(h) || h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".internal")) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(h);
            return addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
