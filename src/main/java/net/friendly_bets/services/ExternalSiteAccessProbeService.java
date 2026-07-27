package net.friendly_bets.services;

import net.friendly_bets.dto.ExternalSiteAccessProbeResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pre-integration smoke check: can this JVM/host fetch a candidate external site
 * without Cloudflare JS challenge (as with aiscore.com from datacenter IPs).
 */
@Service
public class ExternalSiteAccessProbeService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSiteAccessProbeService.class);
    private static final int BODY_SNIPPET_LIMIT = 500;
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ExternalSiteAccessProbeResultDto probe(String urlRaw) {
        URI uri = parseAndValidateUrl(urlRaw);
        String requested = uri.toString();
        long started = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
                    .header("User-Agent", USER_AGENTS.get(ThreadLocalRandom.current().nextInt(USER_AGENTS.size())))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - started;
            String body = response.body() != null ? response.body() : "";
            String server = firstHeader(response, "server");
            String cfRay = firstHeader(response, "cf-ray");
            String cfMitigated = firstHeader(response, "cf-mitigated");
            boolean cloudflare = isCloudflare(server, cfRay, cfMitigated, body);
            boolean jsChallenge = isJsChallenge(body);
            String verdict = resolveVerdict(response.statusCode(), jsChallenge);
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
                    .jsChallengeSuspected(jsChallenge)
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

    private static String resolveVerdict(int status, boolean jsChallenge) {
        if (jsChallenge) {
            return "CLOUDFLARE_JS_CHALLENGE";
        }
        if (status >= 400) {
            return "HTTP_BLOCKED";
        }
        return "PASS";
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

    private static boolean isJsChallenge(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("just a moment")
                || lower.contains("cf-challenge")
                || lower.contains("challenge-platform")
                || lower.contains("checking your browser")
                || lower.contains("enable javascript and cookies")
                || (lower.contains("attention required") && lower.contains("cloudflare"));
    }

    private static String firstHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    private static String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() <= BODY_SNIPPET_LIMIT) {
            return compact;
        }
        return compact.substring(0, BODY_SNIPPET_LIMIT) + "…";
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
            // DNS may fail later during fetch; host string itself is fine
            return false;
        }
    }
}
