package com.aibot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final GroqVisionService groqVisionService;

    public String analyzeImage(byte[] imageContent, String fileName, String question) {
        try {
            log.info("🖼 Начинаем анализ изображения: {}", fileName);
            String base64Image = Base64.getEncoder().encodeToString(imageContent);
            String imageType = getImageType(fileName);
            log.info("📸 Тип изображения: {}", imageType);
            String response = groqVisionService.analyzeImage(base64Image, imageType, question);
            log.info("✅ Анализ изображения завершён");
            return response;
        } catch (Exception e) {
            log.error("❌ Ошибка при анализе изображения: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось проанализировать изображение", e);
        }
    }

    public String analyzeImage(byte[] imageContent, String fileName) {
        return analyzeImage(imageContent, fileName, "Что изображено на этом изображении? Дай подробное описание.");
    }

    public String analyzeScreenshot(byte[] imageContent, String fileName) {
        return analyzeImage(imageContent, fileName,
                "Это скриншот. Помоги понять, что на нём изображено. Прочитай и объясни весь текст, видимый на экране.");
    }

    public String analyzeChart(byte[] imageContent, String fileName) {
        return analyzeImage(imageContent, fileName,
                "Это график или диаграмма. Проанализируй её и объясни, какие данные она показывает, какие тренды видны.");
    }

    public String analyzeTable(byte[] imageContent, String fileName) {
        return analyzeImage(imageContent, fileName,
                "На этом изображении таблица. Прочитай и структурируй данные из таблицы, объясни, что они означают.");
    }

    private String getImageType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    public boolean isImageSizeValid(byte[] imageContent) {
        return imageContent.length <= 5 * 1024 * 1024;
    }
}
