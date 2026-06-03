package net.friendly_bets.marathonbet.client;

import java.net.http.HttpRequest;

public final class MarathonbetPanHeaders {

    public static final String BASE_URL = "https://new.marathonbet.ru";

    private MarathonbetPanHeaders() {
    }

    public static void apply(HttpRequest.Builder builder, String referer) {
        builder.header("Accept-Language", "ru-RU")
                .header("X-Pan-Source", "REDESIGN_WEB")
                .header("X-Pan-Version", "MOBILE-SSR-2.5.4")
                .header("X-Pan-Target", "BROWSER")
                .header("X-Country-Code", "RU")
                .header("User-Agent", "FriendlyBets/1.0 (odds sync)");
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
    }
}
