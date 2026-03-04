package com.aibot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class GigaChatService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gigachat.client-id}")
    private String clientId;

    @Value("${gigachat.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String API_URL  = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.MIN;

    // ✅ Только этот конструктор изменился — добавлен SSL bypass
    public GigaChatService(WebClient.Builder webClientBuilder) {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslSpec -> sslSpec.sslContext(sslContext));

            this.webClient = webClientBuilder
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка настройки SSL", e);
        }
    }

    private String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        try {
            log.info("🔑 Запрос нового токена доступа...");
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            String response = webClient.post()
                    .uri(AUTH_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .header("RqUID", UUID.randomUUID().toString())
                    .header("Authorization", "Basic " + encodedCredentials)
                    .body(BodyInserters.fromFormData("scope", "GIGACHAT_API_PERS"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            cachedToken = root.path("access_token").asText();
            long expiresIn = root.path("expires_at").asLong(1800);
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("✅ Токен успешно получен, истекает через {} сек", expiresIn);
            return cachedToken;

        } catch (WebClientResponseException e) {
            log.error("❌ Ошибка получения токена: {} | Статус: {} | Тело: {}",
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Не удалось получить токен", e);
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при получении токена: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при запросе токена", e);
        }
    }

    public String sendMessage(String userMessage) {
        String token = getAccessToken();
        try {
            log.info("📤 Отправка сообщения в GigaChat");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", "GigaChat:latest");
            ArrayNode messages = payload.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userMessage);

            String payloadStr = objectMapper.writeValueAsString(payload);
            log.debug("📨 Payload размер: {} символов", payloadStr.length());

            String response = webClient.post()
                    .uri(API_URL)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(payloadStr)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("✅ Получен ответ от GigaChat");
                return content;
            } else {
                log.warn("⚠️ Ответ GigaChat пустой");
                return "Ответ GigaChat не получен или пустой.";
            }

        } catch (WebClientResponseException e) {
            log.error("❌ Ошибка отправки сообщения: {} | Статус: {} | Тело: {}",
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            return "Ошибка при обращении к GigaChat API: " + e.getStatusCode().value();
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при отправке сообщения: {}", e.getMessage(), e);
            return "Непредвиденная ошибка: " + e.getMessage();
        }
    }

    public String analyzeImageWithVision(String base64Image, String imageType, String question) {
        String token = getAccessToken();
        try {
            log.info("🖼 Анализ изображения через GigaChat Vision");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", "GigaChat:latest");
            ArrayNode messages = payload.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");

            ArrayNode contentArray = msg.putArray("content");
            ObjectNode textPart = contentArray.addObject();
            textPart.put("type", "text");
            textPart.put("text", question);

            ObjectNode imagePart = contentArray.addObject();
            imagePart.put("type", "image_url");
            ObjectNode imageUrl = imagePart.putObject("image_url");
            imageUrl.put("url", "data:" + imageType + ";base64," + base64Image);

            String payloadStr = objectMapper.writeValueAsString(payload);

            String response = webClient.post()
                    .uri(API_URL)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(payloadStr)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("✅ Анализ изображения успешно выполнен");
                return content;
            } else {
                log.warn("⚠️ Ответ GigaChat пустой при анализе изображения");
                return "Не удалось проанализировать изображение. Попробуйте позже.";
            }

        } catch (WebClientResponseException e) {
            log.error("❌ Ошибка при анализе изображения: {} | Статус: {}", e.getMessage(), e.getStatusCode());
            if (e.getStatusCode().value() == 400) {
                return "❌ GigaChat Vision API временно недоступен. Попробуйте отправить текстовое описание.";
            }
            return "❌ Ошибка при анализе изображения: " + e.getMessage();
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при анализе изображения: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе изображения: " + e.getMessage();
        }
    }

    public String analyzeDocument(String documentContent, String fileName, String question) {
        try {
            log.info("📄 Анализ документа: {}", fileName);

            String truncatedContent = documentContent;
            if (documentContent.length() > 8000) {
                truncatedContent = documentContent.substring(0, 8000) + "\n... [текст обрезан]";
                log.warn("⚠️ Документ обрезан до 8000 символов (оригинал: {} симв.)", documentContent.length());
            }

            String prompt = "Файл: " + fileName + "\n\n"
                    + "Содержимое документа:\n"
                    + truncatedContent + "\n\n"
                    + "Вопрос: " + question;

            return sendMessage(prompt);

        } catch (Exception e) {
            log.error("❌ Ошибка при анализе документа: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при анализе документа", e);
        }
    }
}
