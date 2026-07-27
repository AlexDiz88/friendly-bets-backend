package net.friendly_bets.scrape;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stable browser fingerprint for the lifetime of a scrape HTTP client.
 * Randomizing UA on every request looks more bot-like than a sticky session profile.
 */
public final class BrowserProfile {

    public enum Kind {
        DESKTOP_CHROME,
        MOBILE_CHROME
    }

    private record Fingerprint(
            String userAgent,
            String secChUa,
            String secChUaMobile,
            String secChUaPlatform
    ) {
    }

    private static final List<Fingerprint> DESKTOP = List.of(
            new Fingerprint(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                    "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                    "?0",
                    "\"Windows\""
            ),
            new Fingerprint(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                    "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                    "?0",
                    "\"macOS\""
            ),
            new Fingerprint(
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                    "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                    "?0",
                    "\"Linux\""
            )
    );

    private static final List<Fingerprint> MOBILE = List.of(
            new Fingerprint(
                    "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                    "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                    "?1",
                    "\"Android\""
            ),
            new Fingerprint(
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                    "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                    "?1",
                    "\"Android\""
            )
    );

    private final Fingerprint fingerprint;
    private final String acceptLanguage;

    private BrowserProfile(Fingerprint fingerprint, String acceptLanguage) {
        this.fingerprint = fingerprint;
        this.acceptLanguage = acceptLanguage;
    }

    public static BrowserProfile randomDesktopRu() {
        return new BrowserProfile(pick(DESKTOP), "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
    }

    public static BrowserProfile randomMobileRu() {
        return new BrowserProfile(pick(MOBILE), "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
    }

    private static Fingerprint pick(List<Fingerprint> pool) {
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    public String userAgent() {
        return fingerprint.userAgent();
    }

    public String secChUa() {
        return fingerprint.secChUa();
    }

    public String secChUaMobile() {
        return fingerprint.secChUaMobile();
    }

    public String secChUaPlatform() {
        return fingerprint.secChUaPlatform();
    }

    public String acceptLanguage() {
        return acceptLanguage;
    }
}
