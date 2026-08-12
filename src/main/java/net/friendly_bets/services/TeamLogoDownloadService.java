package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TeamLogoDownloadService {

    private static final Logger log = LoggerFactory.getLogger(TeamLogoDownloadService.class);

    @Value("${upload.path.logo:upload/logo}")
    private String logoDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public void downloadIfMissing(String logoFileKey, String imageUrl) {
        if (logoFileKey == null || logoFileKey.isBlank() || imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        Path target = Path.of(logoDir, logoFileKey.trim().toLowerCase() + ".png");
        try {
            if (Files.exists(target) && Files.size(target) > 0) {
                return;
            }
            Files.createDirectories(target.getParent());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl.trim()))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (compatible; FriendlyBets/1.0)")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("Skip logo download for {}: HTTP {}", logoFileKey, response.statusCode());
                return;
            }
            try (InputStream in = response.body()) {
                Files.copy(in, target);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("Logo download failed for {} from {}: {}", logoFileKey, imageUrl, e.getMessage());
        }
    }
}
