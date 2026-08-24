package net.friendly_bets.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
public class TelegramBackupClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBackupClient.class);
    private static final String API = "https://api.telegram.org/bot";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TelegramBackupClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public int sendDocument(
            String botToken,
            String chatId,
            byte[] zipBytes,
            String filename,
            String caption,
            boolean disableNotification
    ) throws IOException, InterruptedException {
        String boundary = "----FbBackup" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipart(boundary, chatId, filename, zipBytes, caption, disableNotification);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API + botToken + "/sendDocument"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("ok").asBoolean(false)) {
            throw new IOException(telegramError(root, response.statusCode()));
        }
        int messageId = root.path("result").path("message_id").asInt(0);
        if (messageId <= 0) {
            throw new IOException("telegramSendDocumentNoMessageId");
        }
        return messageId;
    }

    public void sendText(String botToken, String chatId, String text) {
        try {
            String form = "chat_id=" + url(chatId)
                    + "&text=" + url(text)
                    + "&disable_notification=true";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API + botToken + "/sendMessage"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("ok").asBoolean(false)) {
                log.warn("telegram sendMessage failed: {}", telegramError(root, response.statusCode()));
            }
        } catch (Exception e) {
            log.warn("telegram sendMessage failed: {}", e.getMessage());
        }
    }

    /**
     * @return {@code true} if the message is gone (deleted now or already missing)
     */
    public boolean deleteMessage(String botToken, String chatId, int messageId) throws IOException, InterruptedException {
        String form = "chat_id=" + url(chatId) + "&message_id=" + messageId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API + botToken + "/deleteMessage"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(response.body());
        if (root.path("ok").asBoolean(false)) {
            return true;
        }
        String description = root.path("description").asText("");
        if (description.toLowerCase().contains("message to delete not found")
                || description.toLowerCase().contains("message can't be found")) {
            return true;
        }
        throw new IOException(telegramError(root, response.statusCode()));
    }

    private static String telegramError(JsonNode root, int status) {
        String description = root.path("description").asText("telegramError");
        return status + " " + description;
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static byte[] buildMultipart(
            String boundary,
            String chatId,
            String filename,
            byte[] zipBytes,
            String caption,
            boolean disableNotification
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String dash = "--" + boundary + "\r\n";
        writeAscii(out, dash);
        writeAscii(out, "Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
        writeAscii(out, chatId + "\r\n");
        writeAscii(out, dash);
        writeAscii(out, "Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
        writeAscii(out, caption + "\r\n");
        writeAscii(out, dash);
        writeAscii(out, "Content-Disposition: form-data; name=\"disable_notification\"\r\n\r\n");
        writeAscii(out, (disableNotification ? "true" : "false") + "\r\n");
        writeAscii(out, dash);
        writeAscii(out, "Content-Disposition: form-data; name=\"document\"; filename=\"" + filename + "\"\r\n");
        writeAscii(out, "Content-Type: application/zip\r\n\r\n");
        out.write(zipBytes);
        writeAscii(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }
}
