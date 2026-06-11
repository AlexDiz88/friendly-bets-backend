package net.friendly_bets.marathonbet.client;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MarathonbetPanHeaders {

    public static final String BASE_URL = "https://new.marathonbet.ru";

    private static final List<String> MOBILE_CHROME_USER_AGENTS = List.of(
            "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 12; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.6261.89 Mobile/15E148 Safari/604.1"
    );

    private MarathonbetPanHeaders() {
    }

    public static void apply(HttpRequest.Builder builder, String referer) {
        builder.header("Accept-Language", "ru-RU,ru;q=0.9")
                .header("X-Pan-Source", "REDESIGN_WEB")
                .header("X-Pan-Version", "MOBILE-SSR-2.5.4")
                .header("X-Pan-Target", "BROWSER")
                .header("X-Country-Code", "RU")
                .header("User-Agent", pickUserAgent());
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
    }

    static String pickUserAgent() {
        return MOBILE_CHROME_USER_AGENTS.get(
                ThreadLocalRandom.current().nextInt(MOBILE_CHROME_USER_AGENTS.size())
        );
    }
}
