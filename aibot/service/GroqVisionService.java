package com.aibot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class GroqVisionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    public GroqVisionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String analyzeImage(String base64Image, String imageType, String question) {
        try {
            log.info("🖼 Анализ изображения через Groq Vision");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
            payload.put("max_tokens", 1024);

            ArrayNode messages = payload.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");

            ArrayNode contentArray = msg.putArray("content");

            ObjectNode textPart = contentArray.addObject();
            textPart.put("type", "text");
            textPart.put("text", question != null ? question : "Опиши что изображено на картинке подробно на русском языке.");

            ObjectNode imagePart = contentArray.addObject();
            imagePart.put("type", "image_url");
            ObjectNode imageUrl = imagePart.putObject("image_url");
            imageUrl.put("url", "data:" + imageType + ";base64," + base64Image);

            String payloadStr = objectMapper.writeValueAsString(payload);

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(payloadStr)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("✅ Анализ изображения через Groq выполнен");
                return content;
            }

            return "Не удалось проанализировать изображение.";

        } catch (Exception e) {
            log.error("❌ Ошибка Groq Vision: {}", e.getMessage(), e);
            return "❌ Ошибка при анализе изображения: " + e.getMessage();
        }
    }
}
