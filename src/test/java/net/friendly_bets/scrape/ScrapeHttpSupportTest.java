package net.friendly_bets.scrape;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrapeHttpSupportTest {

    @Test
    void looksLikeJsChallenge_detectsCloudflareInterstitial() {
        assertTrue(ScrapeHttpSupport.looksLikeJsChallenge("<html>Just a moment... challenge-platform</html>"));
        assertFalse(ScrapeHttpSupport.looksLikeJsChallenge("<html><body>Premier League</body></html>"));
        assertFalse(ScrapeHttpSupport.looksLikeJsChallenge(null));
    }

    @Test
    void looksLikeAuthInterstitial_detectsSberIdWall() {
        String sberWall = """
                <html lang="ru"><head><title>Авторизация SberID</title></head>
                <body><h2>Вход через SberID</h2>
                <p>Пожалуйста, подождите, выполняется перенаправление...</p>
                <script src="/static/auth/_script.js"></script></body></html>
                """;
        assertTrue(ScrapeHttpSupport.looksLikeAuthInterstitial(sberWall));
        assertTrue(ScrapeHttpSupport.looksLikeAccessWall(sberWall));
        assertFalse(ScrapeHttpSupport.looksLikeJsChallenge(sberWall));
        assertFalse(ScrapeHttpSupport.looksLikeAuthInterstitial("<html><body>Premier League</body></html>"));
        assertFalse(ScrapeHttpSupport.looksLikeAuthInterstitial(
                "<html><body><button>Войти через SberID</button></body></html>"));
    }

    @Test
    void looksLikeAccessWall_coversCfAndSberId() {
        assertTrue(ScrapeHttpSupport.looksLikeAccessWall("<html>Just a moment... challenge-platform</html>"));
        assertFalse(ScrapeHttpSupport.looksLikeAccessWall("<html><body>scores</body></html>"));
        assertFalse(ScrapeHttpSupport.looksLikeAccessWall(null));
    }

    @Test
    void classifyHttpStatus_mapsBlockedCodes() {
        assertEquals(ScrapeFailureKind.HTTP_BLOCKED, ScrapeHttpSupport.classifyHttpStatus(403));
        assertEquals(ScrapeFailureKind.HTTP_BLOCKED, ScrapeHttpSupport.classifyHttpStatus(429));
        assertEquals(ScrapeFailureKind.HTTP_BLOCKED, ScrapeHttpSupport.classifyHttpStatus(503));
        assertEquals(ScrapeFailureKind.HTTP_ERROR, ScrapeHttpSupport.classifyHttpStatus(404));
        assertEquals(ScrapeFailureKind.HTTP_ERROR, ScrapeHttpSupport.classifyHttpStatus(500));
    }

    @Test
    void classifyThrowable_detectsTimeout() {
        assertEquals(
                ScrapeFailureKind.TIMEOUT,
                ScrapeHttpSupport.classifyThrowable(new java.net.http.HttpTimeoutException("timed out"))
        );
        assertEquals(
                ScrapeFailureKind.NETWORK_ERROR,
                ScrapeHttpSupport.classifyThrowable(new java.io.IOException("connection reset"))
        );
    }

    @Test
    void decompressIfNeeded_gunzipsByHeaderAndMagic() throws Exception {
        byte[] plain = "<html>Ставки на футбол</html>".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = gzip(plain);
        assertEquals(
                new String(plain, StandardCharsets.UTF_8),
                new String(ScrapeHttpSupport.decompressIfNeeded(gzipped, "gzip"), StandardCharsets.UTF_8)
        );
        assertEquals(
                new String(plain, StandardCharsets.UTF_8),
                new String(ScrapeHttpSupport.decompressIfNeeded(gzipped, null), StandardCharsets.UTF_8)
        );
        assertEquals(
                "plain",
                new String(ScrapeHttpSupport.decompressIfNeeded("plain".getBytes(StandardCharsets.UTF_8), null),
                        StandardCharsets.UTF_8)
        );
    }

    @Test
    void charsetFromContentType_readsExplicitCharset() {
        assertEquals(StandardCharsets.UTF_8, ScrapeHttpSupport.charsetFromContentType(null));
        assertEquals(
                java.nio.charset.Charset.forName("windows-1251"),
                ScrapeHttpSupport.charsetFromContentType("text/html; charset=windows-1251")
        );
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(input);
        }
        return bos.toByteArray();
    }
}
